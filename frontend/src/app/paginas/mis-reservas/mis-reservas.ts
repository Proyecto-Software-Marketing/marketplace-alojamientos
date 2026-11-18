import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { leerMensajes } from '../../servicios/errores';
import { Cancelacion, EstadoReserva, Reserva, ReservasService } from '../../servicios/reservas.service';
import { SesionService } from '../../servicios/sesion.service';

// Los estados que se pueden filtrar en el historial
const ESTADOS: { valor: EstadoReserva | ''; texto: string }[] = [
  { valor: '', texto: 'Todos' },
  { valor: 'CONFIRMADA', texto: 'Confirmadas' },
  { valor: 'COMPLETADA', texto: 'Completadas' },
  { valor: 'CANCELADA', texto: 'Canceladas' },
  { valor: 'EN_REVISION_MANUAL', texto: 'En revisión manual' }
];

// HU-26: historial de reservas del usuario que inició sesión.
// El huésped ve las suyas y el anfitrión las de sus publicaciones.
// HU-28: desde el historial se puede cancelar una reserva.
@Component({
  selector: 'app-mis-reservas',
  imports: [FormsModule, RouterLink],
  templateUrl: './mis-reservas.html',
  styleUrl: './mis-reservas.css'
})
export class MisReservas {

  private reservas = inject(ReservasService);
  private sesion = inject(SesionService);

  readonly estados = ESTADOS;

  items = signal<Reserva[]>([]);
  cargando = signal(true);
  errores = signal<string[]>([]);
  mensaje = signal('');

  estado = signal<EstadoReserva | ''>('');

  // La reserva que se está cancelando y el motivo que escribe el usuario
  cancelando = signal<Reserva | null>(null);
  motivo = '';
  motivoEnviado = signal(false);
  devolucion = signal<Cancelacion | null>(null);

  constructor() {
    this.cargar();
  }

  // HU-26: el historial, de la más reciente a la más antigua
  cargar(): void {
    this.errores.set([]);
    this.cargando.set(true);
    this.reservas.listar(this.estado() || undefined).subscribe({
      next: (lista) => {
        this.items.set(lista.items);
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errores.set(leerMensajes(error));
        this.cargando.set(false);
      }
    });
  }

  // CA-XXX: al cambiar el filtro se vuelve a pedir la lista
  filtrar(estado: EstadoReserva | ''): void {
    this.estado.set(estado);
    this.cargar();
  }

  // HU-28: se abre el formulario de cancelación de la reserva
  abrirCancelacion(reserva: Reserva): void {
    this.errores.set([]);
    this.mensaje.set('');
    this.devolucion.set(null);
    this.motivo = '';
    this.motivoEnviado.set(false);
    this.cancelando.set(reserva);
  }

  cerrarCancelacion(): void {
    this.cancelando.set(null);
    this.motivo = '';
    this.motivoEnviado.set(false);
  }

  // CA-XXX: el motivo debe tener entre 3 y 500 caracteres
  motivoValido(): boolean {
    return this.motivo.trim().length >= 3 && this.motivo.trim().length <= 500;
  }

  // HU-28: se cancela la reserva y se muestra la devolución simulada
  confirmarCancelacion(): void {
    const reserva = this.cancelando();
    if (reserva === null) {
      return;
    }
    this.errores.set([]);
    this.mensaje.set('');

    if (!this.motivoValido()) {
      this.motivoEnviado.set(true);
      return;
    }

    this.reservas.cancelar(reserva.id, this.motivo.trim()).subscribe({
      next: (cancelacion) => {
        this.devolucion.set(cancelacion);
        this.cancelando.set(null);
        this.motivo = '';
        this.motivoEnviado.set(false);
        this.cargar();
      },
      error: (error: HttpErrorResponse) => {
        this.errores.set(leerMensajes(error));
        this.motivoEnviado.set(true);
      }
    });
  }

  // Solo se puede cancelar una reserva que sigue confirmada
  puedeCancelar(reserva: Reserva): boolean {
    return reserva.estado === 'CONFIRMADA';
  }

  // El anfitrión ve las reservas de sus publicaciones
  esAnfitrion(): boolean {
    return this.sesion.usuario()?.rol === 'ANFITRION';
  }

  titulo(): string {
    return this.esAnfitrion() ? 'Reservas de mis publicaciones' : 'Mis reservas';
  }

  textoEstado(estado: EstadoReserva): string {
    return this.estados.find((item) => item.valor === estado)?.texto ?? estado;
  }

  // Las fechas de la reserva vienen en yyyy-MM-dd
    fecha(iso: string): string {
    const [anio, mes, dia] = iso.split('-').map(Number);
    return new Date(anio, mes - 1, dia).toLocaleDateString('es-CO', { dateStyle: 'medium' });
  }

  fechaHora(iso: string): string {
    return new Date(iso).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' });
  }
}
