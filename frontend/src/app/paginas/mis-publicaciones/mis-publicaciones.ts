import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AlojamientosService, Publicacion } from '../../servicios/alojamientos.service';
import { leerMensajes } from '../../servicios/errores';

// Texto que ve el anfitrión para cada estado
const ESTADOS: Record<string, string> = {
  BORRADOR: 'Borrador',
  PENDIENTE_REVISION: 'En revisión',
  ACTIVA: 'Activa',
  PAUSADA: 'Pausada',
  BLOQUEADA: 'Bloqueada'
};

// HU-15 el anfitrión ve sus publicaciones con su estado
@Component({
  selector: 'app-mis-publicaciones',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './mis-publicaciones.html',
  styleUrl: './mis-publicaciones.css'
})
export class MisPublicaciones {

  private alojamientos = inject(AlojamientosService);

  publicaciones = signal<Publicacion[]>([]);
  errores = signal<string[]>([]);
  mensaje = signal('');
  cargando = signal(true);
  // id de la publicación que se está enviando, para bloquear el botón
  enviando = signal<number | null>(null);

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.alojamientos.misPublicaciones().subscribe({
      next: (respuesta) => {
        this.publicaciones.set(respuesta.items);
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errores.set(leerMensajes(error));
        this.cargando.set(false);
      }
    });
  }

  // HU-12 enviar a revisión (si falta algo, el back dice qué falta)
  enviar(publicacion: Publicacion): void {
    this.enviando.set(publicacion.id);
    this.errores.set([]);
    this.mensaje.set('');
    this.alojamientos.enviarARevision(publicacion.id).subscribe({
      next: () => {
        this.enviando.set(null);
        this.mensaje.set(`"${publicacion.titulo}" quedó enviada a revisión.`);
        this.cargar();
      },
      error: (error: HttpErrorResponse) => {
        this.enviando.set(null);
        this.errores.set(leerMensajes(error));
      }
    });
  }

  textoEstado(estado: string): string {
    return ESTADOS[estado] ?? estado;
  }

  // Solo se edita en BORRADOR, ACTIVA o PAUSADA (igual que en el back)
  puedeEditar(estado: string): boolean {
    return estado === 'BORRADOR' || estado === 'ACTIVA' || estado === 'PAUSADA';
  }
}