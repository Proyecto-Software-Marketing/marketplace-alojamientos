import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

// Una notificación interna que devuelve el servicio Notificaciones
export interface Notificacion {
  id: number;
  usuarioId: number;
  tipoEvento: string;
  entidadTipo: string;
  entidadId: number;
  mensaje: string;
  leida: boolean;
  creadaEn: string;
}

// Lista de notificaciones, de la más reciente a la más antigua
export interface ListaNotificaciones {
  items: Notificacion[];
  noLeidas: number;
}

// Llamadas al servicio Notificaciones
@Injectable({ providedIn: 'root' })
export class NotificacionesService {

  private http = inject(HttpClient);
  private url = '/api/notificaciones/notifications';

  // HU-04 buzón simulado de recuperación, sin sesión
  buzonRecuperacion(correo: string) {
    const parametros = new HttpParams().set('correo', correo);
    return this.http.get<ListaNotificaciones>(`${this.url}/password-recovery`, { params: parametros });
  }
}
