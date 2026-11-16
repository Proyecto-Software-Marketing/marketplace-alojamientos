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

// Llamadas al servicio Disponibilidad
@Injectable({ providedIn: 'root' })
export class DisponibilidadService {

  private http = inject(HttpClient);
  private url = '/api/disponibilidad/listings';

  // HU-17 tramos desde hoy: disponible, reservado o bloqueado
  calendario(idPublicacion: number) {
    return this.http.get<Calendario>(`${this.url}/${idPublicacion}/calendar`);
  }
}
