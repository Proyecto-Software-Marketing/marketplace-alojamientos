import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AlojamientosService, Publicacion } from '../../servicios/alojamientos.service';
import { leerMensajes } from '../../servicios/errores';

// HU-13 el administrador aprueba o rechaza las publicaciones pendientes
@Component({
  selector: 'app-aprobaciones',
  imports: [RouterLink, DecimalPipe, FormsModule],
  templateUrl: './aprobaciones.html',
  styleUrl: './aprobaciones.css'
})
export class Aprobaciones {

  private alojamientos = inject(AlojamientosService);

  pendientes = signal<Publicacion[]>([]);
  errores = signal<string[]>([]);
  mensaje = signal('');
  cargando = signal(true);
  procesando = signal(false);

  // publicación a la que se le está escribiendo el motivo del rechazo
  rechazando = signal<number | null>(null);
  motivo = '';

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.alojamientos.pendientes().subscribe({
      next: (respuesta) => {
        this.pendientes.set(respuesta.items);
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errores.set(leerMensajes(error));
        this.cargando.set(false);
      }
    });
  }

  // CA-037 aprobar: pasa a ACTIVA y aparece en las búsquedas
  aprobar(publicacion: Publicacion): void {
    this.limpiarMensajes();
    this.procesando.set(true);
    this.alojamientos.aprobar(publicacion.id).subscribe({
      next: () => this.terminar(`"${publicacion.titulo}" quedó aprobada y activa.`),
      error: (error: HttpErrorResponse) => this.fallo(error)
    });
  }

  abrirRechazo(id: number): void {
    this.limpiarMensajes();
    this.rechazando.set(id);
    this.motivo = '';
  }

  // CA-038 rechazar con motivo: vuelve a BORRADOR y el anfitrión ve el motivo
  rechazar(publicacion: Publicacion): void {
    const motivo = this.motivo.trim();
    if (motivo.length < 3 || motivo.length > 2000) {
      this.errores.set(['El motivo debe tener entre 3 y 2000 caracteres.']);
      return;
    }
    this.limpiarMensajes();
    this.procesando.set(true);
    this.alojamientos.rechazar(publicacion.id, motivo).subscribe({
      next: () => this.terminar(`"${publicacion.titulo}" quedó rechazada y volvió a borrador.`),
      error: (error: HttpErrorResponse) => this.fallo(error)
    });
  }

  private terminar(texto: string): void {
    this.procesando.set(false);
    this.rechazando.set(null);
    this.mensaje.set(texto);
    this.cargar();
  }

  private fallo(error: HttpErrorResponse): void {
    this.procesando.set(false);
    this.errores.set(leerMensajes(error));
  }

  private limpiarMensajes(): void {
    this.errores.set([]);
    this.mensaje.set('');
  }
}