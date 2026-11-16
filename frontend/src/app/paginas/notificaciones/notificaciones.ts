import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { leerMensajes } from '../../servicios/errores';
import { Notificacion, NotificacionesService } from '../../servicios/notificaciones.service';

// Título que se muestra para cada tipo de evento
const TITULOS: Record<string, string> = {
  PUBLICACION_APROBADA: 'Publicación aprobada',
  RESERVA_CONFIRMADA: 'Reserva confirmada',
  RESERVA_CANCELADA: 'Reserva cancelada',
  PUBLICACION_BLOQUEADA: 'Publicación bloqueada',
  USUARIO_RESTRINGIDO: 'Cuenta restringida',
  RECUPERACION_CONTRASENA: 'Recuperación de contraseña'
};

// HU-31 bandeja de notificaciones del usuario que inició sesión
@Component({
  selector: 'app-notificaciones',
  imports: [],
  templateUrl: './notificaciones.html',
  styleUrl: './notificaciones.css'
})
export class Notificaciones {

  private notificaciones = inject(NotificacionesService);

  items = signal<Notificacion[]>([]);
  noLeidas = signal(0);
  cargando = signal(true);
  errores = signal<string[]>([]);

  constructor() {
    this.cargar();
  }

  // CA-085 solo las mías, de la más reciente a la más antigua
  cargar(): void {
    this.notificaciones.listar().subscribe({
      next: (lista) => {
        this.items.set(lista.items);
        this.noLeidas.set(lista.noLeidas);
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errores.set(leerMensajes(error));
        this.cargando.set(false);
      }
    });
  }

  // CA-086 se cambia solo la fila marcada, sin recargar toda la lista
  marcarLeida(notificacion: Notificacion): void {
    this.errores.set([]);
    this.notificaciones.marcarLeida(notificacion.id).subscribe({
      next: (actualizada) => {
        this.items.update((lista) => lista.map((item) => item.id === actualizada.id ? actualizada : item));
        this.noLeidas.update((cantidad) => Math.max(cantidad - 1, 0));
      },
      error: (error: HttpErrorResponse) => {
        this.errores.set(leerMensajes(error));
        // CA-087 si ya no existe o no es mía (404) se vuelve a pedir la lista
        if (error.status === 404) {
          this.cargar();
        }
      }
    });
  }

  titulo(tipoEvento: string): string {
    return TITULOS[tipoEvento] ?? 'Notificación';
  }

  // creadaEn viene en hora de Bogotá, p. ej. 2026-11-14T18:05:00
  fecha(creadaEn: string): string {
    return new Date(creadaEn).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' });
  }
}
