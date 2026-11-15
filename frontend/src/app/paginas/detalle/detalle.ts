import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { BusquedaService, PublicacionDetalle } from '../../servicios/busqueda.service';
import { leerMensajes } from '../../servicios/errores';


@Component({
  selector: 'app-detalle',
  imports: [FormsModule, RouterLink, DecimalPipe],
  templateUrl: './detalle.html',
  styleUrl: './detalle.css'
})
export class Detalle {

  private busqueda = inject(BusquedaService);
  private ruta = inject(ActivatedRoute);

  publicacion = signal<PublicacionDetalle | null>(null);
  errores = signal<string[]>([]);


  llegada = '';
  salida = '';
  huespedes = 1;

  constructor() {
    const id = Number(this.ruta.snapshot.paramMap.get('id'));
    const parametros = this.ruta.snapshot.queryParamMap;
    this.llegada = parametros.get('llegada') ?? '';
    this.salida = parametros.get('salida') ?? '';
    this.huespedes = Number(parametros.get('huespedes') ?? 1);

    this.busqueda.detalle(id, this.llegada, this.salida, String(this.huespedes)).subscribe({
      next: (respuesta) => this.publicacion.set(respuesta.publicacion),
      
      error: (error: HttpErrorResponse) => this.errores.set(leerMensajes(error))
    });
  }

  noches(): number {
    if (!this.llegada || !this.salida) {
      return 0;
    }
    const unDia = 24 * 60 * 60 * 1000;
    return Math.round((new Date(this.salida).getTime() - new Date(this.llegada).getTime()) / unDia);
  }
}