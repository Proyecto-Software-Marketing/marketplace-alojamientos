import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export type EstadoTramo = 'DISPONIBLE' | 'RESERVADO' | 'BLOQUEADO';

// Tramo del calendario; id es el periodo disponible al que pertenece
export interface TramoCalendario {
  id: number;
  fechaInicio: string;
  fechaFin: string;
  estado: EstadoTramo;
}

export interface Calendario {
  idPublicacion: number;
  periodos: TramoCalendario[];
}

// Periodo disponible que registra el anfitrión; fechaFin es el día de salida
export interface Periodo {
  id: number;
  idPublicacion: number;
  fechaInicio: string;
  fechaFin: string;
  estado: EstadoTramo;
}

export interface DatosPeriodo {
  fechaInicio: string;
  fechaFin: string;
}

// Llamadas al servicio Disponibilidad
@Injectable({ providedIn: 'root' })
export class DisponibilidadService {

  private http = inject(HttpClient);
  private url = '/api/disponibilidad/listings';

  // HU-17 tramos desde hoy: disponible, reservado o bloqueado
  calendario(idPublicacion: number) {
    return this.http.get<Calendario>(`${this.url}/${idPublicacion}/calendar`);
  }

  // HU-16 el anfitrión dueño registra, edita y elimina periodos
  registrarPeriodo(idPublicacion: number, datos: DatosPeriodo) {
    return this.http.post<Periodo>(`${this.url}/${idPublicacion}/availability`, datos);
  }

  editarPeriodo(idPublicacion: number, idPeriodo: number, datos: DatosPeriodo) {
    return this.http.put<Periodo>(`${this.url}/${idPublicacion}/availability/${idPeriodo}`, datos);
  }

  eliminarPeriodo(idPublicacion: number, idPeriodo: number) {
    return this.http.delete<void>(`${this.url}/${idPublicacion}/availability/${idPeriodo}`);
  }
}
