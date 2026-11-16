import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { leerMensajes } from '../../servicios/errores';
import { Resena, ResenasService } from '../../servicios/resenas.service';
import { SesionService } from '../../servicios/sesion.service';

// HU-33 reseñas y promedio de una publicación; va dentro del detalle del alojamiento.
// HU-36 el administrador puede ocultar una reseña
@Component({
  selector: 'app-resenas-publicacion',
  imports: [FormsModule],
  templateUrl: './resenas-publicacion.html',
  styleUrl: './resenas-publicacion.css'
})
export class ResenasPublicacion {

  private resenas = inject(ResenasService);
  private sesion = inject(SesionService);

  // id que le pasa el detalle: <app-resenas-publicacion [idPublicacion]="p.id" />
  idPublicacion = input.required<number>();

  promedio = signal<number | null>(null);
  items = signal<Resena[]>([]);
  cargando = signal(true);
  errores = signal<string[]>([]);

  // HU-36 solo el administrador ve el botón Ocultar
  esAdministrador = computed(() => this.sesion.usuario()?.rol === 'ADMINISTRADOR');
  ocultando = signal<number | null>(null);
  motivo = '';
  erroresOcultar = signal<string[]>([]);
  mensajeExito = signal('');

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

  abrirOcultar(idResena: number): void {
    this.mensajeExito.set('');
    this.erroresOcultar.set([]);
    this.motivo = '';
    this.ocultando.set(idResena);
  }

  cancelarOcultar(): void {
    this.ocultando.set(null);
    this.erroresOcultar.set([]);
  }

  // CA-100 la reseña deja de verse y de contar en el promedio
  ocultar(idResena: number): void {
    const motivo = this.motivo.trim();
    if (motivo.length < 3 || motivo.length > 500) {
      this.erroresOcultar.set(['El motivo debe tener entre 3 y 500 caracteres']);
      return;
    }
    this.resenas.ocultar(idResena, motivo).subscribe({
      next: () => {
        this.ocultando.set(null);
        this.mensajeExito.set('La reseña quedó oculta.');
        // se vuelve a pedir la lista para que el promedio lo calcule el backend
        this.cargar(this.idPublicacion());
      },
      error: (error: HttpErrorResponse) => this.erroresOcultar.set(leerMensajes(error))
    });
  }
}
