import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Cotizacion as CotizacionDesglosada, CotizacionService } from '../../servicios/cotizacion.service';
import { leerMensajes } from '../../servicios/errores';
import { Reserva, ReservasService } from '../../servicios/reservas.service';
import { SesionService } from '../../servicios/sesion.service';
import { BusquedaService } from '../../servicios/busqueda.service';

// HU-22 cotización desglosada; la abre "Ver cotización" del detalle con
// ?idPublicacion=1&llegada=2026-12-01&salida=2026-12-04&huespedes=2
// HU-23 debajo del desglose se confirma la reserva con esa cotización
@Component({
  selector: 'app-cotizacion',
  imports: [FormsModule, RouterLink, DecimalPipe],
  templateUrl: './cotizacion.html',
  styleUrl: './cotizacion.css'
})
export class Cotizacion {

  private cotizaciones = inject(CotizacionService);
  private reservas = inject(ReservasService);
  private sesion = inject(SesionService);
  private ruta = inject(ActivatedRoute);
  private busqueda = inject(BusquedaService);
  adicionalesDisponibles = signal<{ nombre: string; costo: number }[]>([]);
  elegidos: string[] = [];

  esHuesped = computed(() => this.sesion.usuario()?.rol === 'HUESPED');

  idPublicacion = Number(this.ruta.snapshot.queryParamMap.get('idPublicacion'));
  llegada = this.ruta.snapshot.queryParamMap.get('llegada') ?? '';
  salida = this.ruta.snapshot.queryParamMap.get('salida') ?? '';
  huespedes = Number(this.ruta.snapshot.queryParamMap.get('huespedes') ?? 1);

  cotizacion = signal<CotizacionDesglosada | null>(null);
  errores = signal<string[]>([]);
  cargando = signal(false);

  // HU-23 pago simulado: tokens de tarjetas de prueba de Stripe, sin cobro real (HU-25 conecta Stripe)
  readonly metodosPago = [
    { valor: 'pm_card_visa', texto: 'Visa de prueba' },
    { valor: 'pm_card_mastercard', texto: 'Mastercard de prueba' },
    { valor: 'pm_card_amex', texto: 'American Express de prueba' }
  ];
  metodoPago = 'pm_card_visa';
  reservando = signal(false);
  reserva = signal<Reserva | null>(null);
  erroresReserva = signal<string[]>([]);
  periodoOcupado = signal(false);
  // Una clave por intento de reserva: se reutiliza si el usuario reintenta,
  // así un doble envío no crea dos reservas (RF-026)
  private claveIdempotencia: string | null = null;

  constructor() {
        if (this.idPublicacion) {
      this.busqueda.detalle(this.idPublicacion, '', '', '').subscribe({
        next: (respuesta) => this.adicionalesDisponibles.set(respuesta.publicacion.serviciosAdicionales ?? []),
        error: () => this.adicionalesDisponibles.set([])
      });
    }
    // si el detalle mandó todos los datos se cotiza de una vez
    if (this.esHuesped() && this.idPublicacion && this.llegada && this.salida && this.huespedes) {
      this.cotizar();
    }
  }

  // CA-060 a CA-063: las reglas y los rechazos los decide el backend
  cotizar(): void {
    if (!this.idPublicacion) {
      this.errores.set(['Abre la cotización desde el detalle de un alojamiento']);
      return;
    }
    if (!this.llegada || !this.salida || !this.huespedes) {
      this.errores.set(['Elige las fechas de llegada y salida y el número de huéspedes']);
      return;
    }
    this.errores.set([]);
    this.cotizacion.set(null);
    this.limpiarReserva();
    this.cargando.set(true);
    this.cotizaciones.cotizar({
      publicacionId: this.idPublicacion,
      fechaLlegada: this.llegada,
      fechaSalida: this.salida,
       cantidadHuespedes: this.huespedes,
      serviciosAdicionales: [...this.elegidos]
    }).subscribe({
      next: (cotizacion) => {
        this.cotizacion.set(cotizacion);
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errores.set(leerMensajes(error));
        this.cargando.set(false);
      }
    });
  }

  cambiarAdicional(nombre: string, marcado: boolean): void {
    if (marcado) {
      this.elegidos = [...this.elegidos, nombre];
    } else {
      this.elegidos = this.elegidos.filter((n) => n !== nombre);
    }
    this.cotizar();
  }
  // Suma de los servicios adicionales para la línea del desglose
  totalAdicionales(cotizacion: CotizacionDesglosada): number {
    return cotizacion.serviciosAdicionales.reduce((suma, servicio) => suma + servicio.costo, 0);
  }

  // Lo que queda antes de la tarifa de servicio
  subtotal(cotizacion: CotizacionDesglosada): number {
    return cotizacion.total - cotizacion.tarifaServicio;
  }

  fecha(texto: string): string {
    const [anio, mes, dia] = texto.split('-').map(Number);
    return new Date(anio, mes - 1, dia).toLocaleDateString('es-CO', { dateStyle: 'medium' });
  }

  // CA-064 la reserva queda CONFIRMADA sin aprobación del anfitrión;
  // CA-066 el backend bloquea el periodo y notifica, la pantalla solo informa
  confirmarReserva(cotizacion: CotizacionDesglosada): void {
    if (this.claveIdempotencia === null) {
      this.claveIdempotencia = crypto.randomUUID();
    }
    this.erroresReserva.set([]);
    this.periodoOcupado.set(false);
    this.reservando.set(true);
    this.reservas.crearReserva(cotizacion.id, this.metodoPago, this.claveIdempotencia).subscribe({
      next: (reserva) => {
        this.reserva.set(reserva);
        this.reservando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.reservando.set(false);
        this.erroresReserva.set(this.mensajeReserva(error));
      }
    });
  }

  private mensajeReserva(error: HttpErrorResponse): string[] {
    switch (error.status) {
      case 409:
        // CA-065 otro huésped reservó el periodo antes
        this.periodoOcupado.set(true);
        return ['Este periodo ya no está disponible'];
      case 401:
      case 403:
        return ['Debes iniciar sesión como huésped'];
      case 402:
        // previsto para cuando exista el pago real (HU-25)
        return ['El pago fue rechazado'];
      default:
        // 400 y los demás: los mensajes del backend
        return leerMensajes(error);
    }
  }

  // Una cotización nueva es un intento de reserva nuevo
  private limpiarReserva(): void {
    this.reserva.set(null);
    this.erroresReserva.set([]);
    this.periodoOcupado.set(false);
    this.claveIdempotencia = null;
  }
}
