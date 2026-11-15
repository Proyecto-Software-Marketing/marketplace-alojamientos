import { Injectable, signal } from '@angular/core';

// Datos del usuario que devuelve 
export interface Usuario {
  id: number;
  nombre: string;
  correo: string;
  rol: 'HUESPED' | 'ANFITRION' | 'ADMINISTRADOR';
  estado: string;
}

// Guarda el token y el usuario despues del login 
@Injectable({ providedIn: 'root' })
export class SesionService {

  // el usuario que inicio sesion, o null si no hay nadie
  usuario = signal<Usuario | null>(this.leerUsuario());

  guardar(token: string, usuario: Usuario): void {
    localStorage.setItem('token', token);
    localStorage.setItem('usuario', JSON.stringify(usuario));
    this.usuario.set(usuario);
  }

  token(): string | null {
    return localStorage.getItem('token');
  }

  cerrar(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('usuario');
    this.usuario.set(null);
  }

  estaLogueado(): boolean {
    return this.usuario() !== null;
  }

  esAdmin(): boolean {
    return this.usuario()?.rol === 'ADMINISTRADOR';
  }

  private leerUsuario(): Usuario | null {
    const texto = localStorage.getItem('usuario');
    return texto ? JSON.parse(texto) : null;
  }
}