import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Cotizacion as CotizacionDesglosada, CotizacionService } from '../../servicios/cotizacion.service';
import { leerMensajes } from '../../servicios/errores';
import { SesionService } from '../../servicios/sesion.service';

// HU-22 cotización desglosada; la abre "Ver cotización" del detalle con
// ?idPublicacion=1&llegada=2026-12-01&salida=2026-12-04&huespedes=2
@Component({
  selector: 'app-cotizacion',
  imports: [FormsModule, RouterLink, DecimalPipe],
  templateUrl: './cotizacion.html',
  styleUrl: './cotizacion.css'
})
export class Cotizacion {

  private cotizaciones = inject(CotizacionService);
  private sesion = inject(SesionService);
  private ruta = inject(ActivatedRoute);

  esHuesped = computed(() => this.sesion.usuario()?.rol === 'HUESPED');

  idPublicacion = Number(this.ruta.snapshot.queryParamMap.get('idPublicacion'));
  llegada = this.ruta.snapshot.queryParamMap.get('llegada') ?? '';
  salida = this.ruta.snapshot.queryParamMap.get('salida') ?? '';
  huespedes = Number(this.ruta.snapshot.queryParamMap.get('huespedes') ?? 1);

  cotizacion = signal<CotizacionDesglosada | null>(null);
  errores = signal<string[]>([]);
  cargando = signal(false);

  constructor() {
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
    this.cargando.set(true);
    this.cotizaciones.cotizar({
      publicacionId: this.idPublicacion,
      fechaLlegada: this.llegada,
      fechaSalida: this.salida,
      cantidadHuespedes: this.huespedes
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
}
