import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

// Una reseña tal como la devuelve el servicio Reseñas
export interface Resena {
  id: number;
  reservaId: number;
  huespedId: number;
  publicacionId: number;
  calificacion: number;
  contenido: string;
  visible: boolean;
  fecha: string;
}

export interface DatosResena {
  calificacion: number;
  contenido: string;
}

// Llamadas al servicio Reseñas
@Injectable({ providedIn: 'root' })
export class ResenasService {

  private http = inject(HttpClient);
  private url = '/api/resenas';

  // HU-32 el huésped califica y reseña una estancia completada
  calificar(idReserva: number, datos: DatosResena) {
    return this.http.post<Resena>(`${this.url}/bookings/${idReserva}/review`, datos);
  }
}
