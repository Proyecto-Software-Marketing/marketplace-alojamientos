import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BusquedaService, FiltrosBusqueda, PublicacionResumen } from '../../servicios/busqueda.service';
import { leerMensajes } from '../../servicios/errores';


@Component({
  selector: 'app-inicio',
  imports: [FormsModule, RouterLink, DecimalPipe],
  templateUrl: './inicio.html',
  styleUrl: './inicio.css'
})
export class Inicio {

  private busqueda = inject(BusquedaService);

  ciudad = '';
  llegada = '';
  salida = '';
  huespedes = 1;
  tipo = '';
  precioMinimo: number | null = null;
  precioMaximo: number | null = null;
  serviciosElegidos: string[] = [];

  tipos = ['Apartamento', 'Casa', 'Habitación', 'Cabaña', 'Otro'];
  servicios = ['Wifi', 'Cocina', 'Aire acondicionado', 'Lavadora', 'Aparcamiento', 'Piscina'];


  resultados = signal<PublicacionResumen[]>([]);
  mensaje = signal('');
  errores = signal<string[]>([]);
  ultimaBusqueda = signal<FiltrosBusqueda | null>(null);
    destacadas = signal<PublicacionResumen[]>([]);

  constructor() {
    this.busqueda.destacadas().subscribe({
      next: (lista) => this.destacadas.set(lista),
      error: () => this.destacadas.set([])
    });
  }


  cambiarServicio(servicio: string, marcado: boolean): void {
    if (marcado) {
      this.serviciosElegidos.push(servicio);
    } else {
      this.serviciosElegidos = this.serviciosElegidos.filter((s) => s !== servicio);
    }
  }

  buscar(): void {
    this.errores.set([]);
    this.mensaje.set('');
    const filtros: FiltrosBusqueda = {
      ciudad: this.ciudad,
      llegada: this.llegada,
      salida: this.salida,
      huespedes: this.huespedes,
      tipo: this.tipo.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase(),
      precioMinimo: this.precioMinimo,
      precioMaximo: this.precioMaximo,
      servicios: [...this.serviciosElegidos]
    };
    this.busqueda.buscar(filtros).subscribe({
      next: (respuesta) => {
        this.resultados.set(respuesta.resultados);
     
        this.mensaje.set(respuesta.mensaje ?? '');
        this.ultimaBusqueda.set(filtros);
      },
      error: (error: HttpErrorResponse) => {
        this.resultados.set([]);
        this.ultimaBusqueda.set(null);
        this.errores.set(leerMensajes(error));
      }
    });
  }


  noches(): number {
    const datos = this.ultimaBusqueda();
    if (!datos) {
      return 0;
    }
    const unDia = 24 * 60 * 60 * 1000;
    return Math.round((new Date(datos.salida).getTime() - new Date(datos.llegada).getTime()) / unDia);
  }
}