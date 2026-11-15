import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Usuario } from './sesion.service';

// Llamadas al servicio Identidad 
@Injectable({ providedIn: 'root' })
export class IdentidadService {

  private http = inject(HttpClient);
  private url = '/api/identidad';

  // HU-01
  registrar(datos: { nombre: string; correo: string; contrasena: string; rol: string }) {
    return this.http.post<Usuario>(`${this.url}/autenticacion/registro`, datos);
  }

  // HU-02
  login(correo: string, contrasena: string) {
    return this.http.post<{ token: string; usuario: Usuario }>(`${this.url}/autenticacion/login`, { correo, contrasena });
  }

  logout() {
    return this.http.post<void>(`${this.url}/autenticacion/logout`, {});
  }
}