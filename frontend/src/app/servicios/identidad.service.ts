import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Usuario } from './sesion.service';


export interface RespuestaSesion {
  token: string;
  usuario: Usuario;
}

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
    return this.http.post<RespuestaSesion>(`${this.url}/autenticacion/login`, { correo, contrasena });
  }

  logout() {
    return this.http.post<void>(`${this.url}/autenticacion/logout`, {});
  }

  // HU-05
  consultarPerfil() {
    return this.http.get<Usuario>(`${this.url}/usuarios/perfil`);
  }

  actualizarPerfil(nombre: string, correo: string) {
    return this.http.put<Usuario>(`${this.url}/usuarios/perfil`, { nombre, correo });
  }

  // HU-06
  desactivarCuenta() {
    return this.http.post<void>(`${this.url}/usuarios/desactivar`, {});
  }

  reactivarCuenta(correo: string, contrasena: string) {
    return this.http.post<RespuestaSesion>(`${this.url}/autenticacion/reactivar`, { correo, contrasena });
  }
}