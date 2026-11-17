import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export interface ListaFavoritos {
  publicaciones: number[];
}

@Injectable({ providedIn: 'root' })
export class FavoritosService {

  private http = inject(HttpClient);
  private url = '/api/identidad/usuarios/favoritos';

  listar() {
    return this.http.get<ListaFavoritos>(this.url);
  }

 
  agregar(idPublicacion: number) {
    return this.http.post<ListaFavoritos>(`${this.url}/${idPublicacion}`, {});
  }

  quitar(idPublicacion: number) {
    return this.http.delete<ListaFavoritos>(`${this.url}/${idPublicacion}`);
  }
}