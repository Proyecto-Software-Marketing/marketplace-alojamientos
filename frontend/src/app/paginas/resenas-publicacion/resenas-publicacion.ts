import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, input, signal } from '@angular/core';
import { leerMensajes } from '../../servicios/errores';
import { Resena, ResenasService } from '../../servicios/resenas.service';

// HU-33 reseñas y promedio de una publicación; va dentro del detalle del alojamiento
@Component({
  selector: 'app-resenas-publicacion',
  imports: [],
  templateUrl: './resenas-publicacion.html',
  styleUrl: './resenas-publicacion.css'
})
export class ResenasPublicacion {

  private resenas = inject(ResenasService);

  // id que le pasa el detalle: <app-resenas-publicacion [idPublicacion]="p.id" />
  idPublicacion = input.required<number>();

  promedio = signal<number | null>(null);
  items = signal<Resena[]>([]);
  cargando = signal(true);
  errores = signal<string[]>([]);

  constructor() {
    // se vuelve a pedir si cambia el id de la publicación
    effect(() => this.cargar(this.idPublicacion()));
  }

  // CA-092 promedio y reseñas visibles, la más reciente primero; CA-093 las ocultas no llegan
  cargar(idPublicacion: number): void {
    this.cargando.set(true);
    this.errores.set([]);
    this.resenas.listar(idPublicacion).subscribe({
      next: (lista) => {
        this.promedio.set(lista.calificacionPromedio);
        this.items.set(lista.items);
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errores.set(leerMensajes(error));
        this.cargando.set(false);
      }
    });
  }

  // 4.333 se muestra como 4,3
  promedioTexto(promedio: number): string {
    return promedio.toLocaleString('es-CO', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
  }

  // 4 se muestra como ★★★★☆
  estrellas(calificacion: number): string {
    const llenas = Math.round(calificacion);
    return '★'.repeat(llenas) + '☆'.repeat(5 - llenas);
  }

  fecha(texto: string): string {
    return new Date(texto).toLocaleDateString('es-CO', { dateStyle: 'medium' });
  }
}
