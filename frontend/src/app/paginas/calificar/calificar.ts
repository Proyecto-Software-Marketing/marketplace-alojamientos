import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { leerMensajes } from '../../servicios/errores';
import { ResenasService } from '../../servicios/resenas.service';
import { Reserva, ReservasService } from '../../servicios/reservas.service';

// HU-32 el huésped califica y reseña una estancia completada
@Component({
  selector: 'app-calificar',
  imports: [FormsModule, RouterLink],
  templateUrl: './calificar.html',
  styleUrl: './calificar.css'
})
export class Calificar {

  private reservas = inject(ReservasService);
  private resenas = inject(ResenasService);
  private ruta = inject(ActivatedRoute);

  readonly estrellas = [1, 2, 3, 4, 5];
  readonly maximo = 1000;

  idReserva = Number(this.ruta.snapshot.paramMap.get('idReserva'));
  reserva = signal<Reserva | null>(null);
  cargando = signal(true);
  errores = signal<string[]>([]);
  publicada = signal(false);

  calificacion = signal(0);
  contenido = '';

  constructor() {
    // Resumen de la reserva con el servicio de Reservas de Nicole
    this.reservas.consultar(this.idReserva).subscribe({
      next: (reserva) => {
        this.reserva.set(reserva);
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errores.set(leerMensajes(error));
        this.cargando.set(false);
      }
    });
  }

  elegir(valor: number): void {
    this.calificacion.set(valor);
  }

  // CA-088 publicar la reseña; CA-089, CA-090 y CA-091 los valida el backend
  publicar(): void {
    this.errores.set([]);
    const contenido = this.contenido.trim();
    if (this.calificacion() === 0) {
      this.errores.set(['Elige una calificación de 1 a 5 estrellas']);
      return;
    }
    if (contenido.length === 0) {
      this.errores.set(['Escribe tu reseña']);
      return;
    }
    this.resenas.calificar(this.idReserva, { calificacion: this.calificacion(), contenido }).subscribe({
      next: () => this.publicada.set(true),
      error: (error: HttpErrorResponse) => this.errores.set(leerMensajes(error))
    });
  }

  // Las fechas de la reserva vienen en yyyy-MM-dd
  fecha(texto: string): string {
    const [anio, mes, dia] = texto.split('-').map(Number);
    return new Date(anio, mes - 1, dia).toLocaleDateString('es-CO', { dateStyle: 'medium' });
  }
}
