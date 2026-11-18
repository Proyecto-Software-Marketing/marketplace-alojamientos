import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { IdentidadService } from '../../servicios/identidad.service';
import { Usuario } from '../../servicios/sesion.service';
import { leerMensajes } from '../../servicios/errores';

// HU-35 el administrador restringe o reactiva cuentas
@Component({
  selector: 'app-usuarios',
  imports: [],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.css'
})
export class Usuarios {

  private identidad = inject(IdentidadService);

  usuarios = signal<Usuario[]>([]);
  errores = signal<string[]>([]);
  mensaje = signal('');
  cargando = signal(true);
  // id del usuario que se está cambiando, para bloquear el botón
  procesando = signal<number | null>(null);

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.identidad.listarUsuarios().subscribe({
      next: (lista) => {
        this.usuarios.set(lista);
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errores.set(leerMensajes(error));
        this.cargando.set(false);
      }
    });
  }

  restringir(usuario: Usuario): void {
    this.cambiar(usuario, this.identidad.restringirUsuario(usuario.id), 'quedó restringida');
  }

  reactivar(usuario: Usuario): void {
    this.cambiar(usuario, this.identidad.reactivarUsuario(usuario.id), 'quedó activa otra vez');
  }

  private cambiar(usuario: Usuario, peticion: Observable<Usuario>, texto: string): void {
    this.procesando.set(usuario.id);
    this.errores.set([]);
    this.mensaje.set('');
    peticion.subscribe({
      next: () => {
        this.procesando.set(null);
        this.mensaje.set(`La cuenta de ${usuario.nombre} ${texto}.`);
        this.cargar();
      },
      error: (error: HttpErrorResponse) => {
        this.procesando.set(null);
        this.errores.set(leerMensajes(error));
      }
    });
  }
}