import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export type EstadoPublicacion = 'BORRADOR' | 'PENDIENTE_REVISION' | 'ACTIVA' | 'PAUSADA' | 'BLOQUEADA';
export interface PublicacionInput {
  titulo: string; descripcion: string; ciudad: string; ubicacionTextual: string;
  tipoAlojamiento: string; capacidad: number; precioPorNoche: number;
}
export interface Publicacion extends PublicacionInput {
  id: string; anfitrionId: string; estado: EstadoPublicacion;
  servicios?: string[]; reglas?: string[]; imagenes?: string[];
  motivoRechazo?: string;
}
export interface ListaPublicaciones { items: Publicacion[]; }

@Injectable({ providedIn: 'root' })
export class PublicacionesService {
  private http = inject(HttpClient);
  private base = '/api/alojamientos';
  misPublicaciones() { return this.http.get<ListaPublicaciones>(`${this.base}/listings/mine`); }
  consultar(id: string) { return this.http.get<Publicacion>(`${this.base}/listings/${encodeURIComponent(id)}`); }
  crear(datos: PublicacionInput) { return this.http.post<Publicacion>(`${this.base}/listings`, datos); }
  editar(id: string, datos: PublicacionInput) { return this.http.put<Publicacion>(`${this.base}/listings/${encodeURIComponent(id)}`, datos); }
  enviarRevision(id: string) { return this.http.post<Publicacion>(`${this.base}/listings/${encodeURIComponent(id)}/submit`, {}); }
  pendientes() { return this.http.get<ListaPublicaciones>(`${this.base}/admin/listings`, {params: new HttpParams().set('estado','PENDIENTE_REVISION')}); }
  aprobar(id: string) { return this.http.post<Publicacion>(`${this.base}/admin/listings/${encodeURIComponent(id)}/approve`, {}); }
  rechazar(id: string, motivo: string) { return this.http.post<Publicacion>(`${this.base}/admin/listings/${encodeURIComponent(id)}/reject`, {motivo}); }
}
