import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';


export interface PublicacionResumen {
  id: number;
  titulo: string;
  ciudad: string;
  tipo: string;
  capacidad: number;
  precioNoche: number;
  estado: string;
  servicios: string[];
  imagenPrincipal?: string | null;
}


export interface ResultadoBusqueda {
  resultados: PublicacionResumen[];
  mensaje: string | null;
}


export interface FiltrosBusqueda {
  ciudad: string;
  llegada: string;
  salida: string;
  huespedes: number;
  tipo: string;
  precioMinimo: number | null;
  precioMaximo: number | null;
  servicios: string[];
}


export interface PublicacionDetalle extends PublicacionResumen {
  descripcion: string;
  reglas: string[];
  imagenes: string[];
  politicaCancelacion: string;
  serviciosAdicionales?: { nombre: string; costo: number }[];
}


export interface DetalleRespuesta {
  publicacion: PublicacionDetalle;
  llegada: string | null;
  salida: string | null;
  huespedes: number | null;
}


@Injectable({ providedIn: 'root' })
export class BusquedaService {

  private http = inject(HttpClient);
  private url = '/api/busqueda/busqueda';

  
  buscar(filtros: FiltrosBusqueda) {
    let parametros = new HttpParams()
      .set('ciudad', filtros.ciudad)
      .set('llegada', filtros.llegada)
      .set('salida', filtros.salida)
      .set('huespedes', filtros.huespedes);
    if (filtros.tipo) {
      parametros = parametros.set('tipo', filtros.tipo);
    }
    if (filtros.precioMinimo !== null) {
      parametros = parametros.set('precioMinimo', filtros.precioMinimo);
    }
    if (filtros.precioMaximo !== null) {
      parametros = parametros.set('precioMaximo', filtros.precioMaximo);
    }
    for (const servicio of filtros.servicios) {
      parametros = parametros.append('servicios', servicio);
    }
    return this.http.get<ResultadoBusqueda>(`${this.url}/publicaciones`, { params: parametros });
  }

  
  detalle(id: number, llegada: string, salida: string, huespedes: string) {
    let parametros = new HttpParams();
    if (llegada) {
      parametros = parametros.set('llegada', llegada);
    }
    if (salida) {
      parametros = parametros.set('salida', salida);
    }
    if (huespedes) {
      parametros = parametros.set('huespedes', huespedes);
    }
    return this.http.get<DetalleRespuesta>(`${this.url}/publicaciones/${id}`, { params: parametros });
  }
  destacadas() {
    return this.http.get<PublicacionResumen[]>(`${this.url}/publicaciones/destacadas`);
  }
}