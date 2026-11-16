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

// Reseñas visibles de una publicación con su promedio (null si no hay)
export interface ListaResenas {
  calificacionPromedio: number | null;
  items: Resena[];
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

  // HU-33 reseñas visibles y promedio de una publicación, sin sesión
  listar(idPublicacion: number) {
    return this.http.get<ListaResenas>(`${this.url}/listings/${idPublicacion}/reviews`);
  }

  // HU-36 el administrador oculta una reseña con su motivo
  ocultar(idResena: number, motivo: string) {
    return this.http.post<Resena>(`${this.url}/admin/reviews/${idResena}/hide`, { motivo });
  }
}
