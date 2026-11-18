import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { BusquedaService, PublicacionDetalle } from '../../servicios/busqueda.service';
import { FavoritosService } from '../../servicios/favoritos.service';
import { leerMensajes } from '../../servicios/errores';

@Component({
  selector: 'app-favoritos',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './favoritos.html',
  styleUrl: './favoritos.css'
})
export class Favoritos {

  private favoritos = inject(FavoritosService);
  private busqueda = inject(BusquedaService);

  publicaciones = signal<PublicacionDetalle[]>([]);
  errores = signal<string[]>([]);
  cargando = signal(true);

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.favoritos.listar().subscribe({
      next: (lista) => {
        if (lista.publicaciones.length === 0) {
          this.cargando.set(false);
          return;
        }
       
        const peticiones = lista.publicaciones.map((id) =>
          this.busqueda.detalle(id, '', '', '').pipe(catchError(() => of(null)))
        );
        forkJoin(peticiones).subscribe((respuestas) => {
          const encontradas = respuestas
            .filter((respuesta) => respuesta !== null)
            .map((respuesta) => respuesta.publicacion);
          this.publicaciones.set(encontradas);
          this.cargando.set(false);
        });
      },
      error: (error: HttpErrorResponse) => {
        this.errores.set(leerMensajes(error));
        this.cargando.set(false);
      }
    });
  }

  
  quitar(id: number): void {
    this.favoritos.quitar(id).subscribe({
      next: () => this.publicaciones.update((lista) => lista.filter((p) => p.id !== id)),
      error: (error: HttpErrorResponse) => this.errores.set(leerMensajes(error))
    });
  }
}