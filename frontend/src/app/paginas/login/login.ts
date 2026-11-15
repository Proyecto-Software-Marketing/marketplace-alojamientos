import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {ActivatedRoute, Router } from '@angular/router';
import { leerMensajes } from '../../servicios/errores';
import { IdentidadService } from '../../servicios/identidad.service';
import { SesionService } from '../../servicios/sesion.service';
import { Registro } from '../registro/registro';



@Component({
  selector: 'app-login',
  imports: [FormsModule, Registro],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {

  private identidad = inject(IdentidadService);
  private sesion = inject(SesionService);
  private router = inject(Router);
  private ruta = inject(ActivatedRoute);

  constructor() {
    this.ruta.queryParamMap.subscribe((parametros) => {
      this.modo.set(parametros.get('modo') === 'registro' ? 'registro' : 'login');
    });
  }


  modo = signal<'login' | 'registro'>('login');

  correo = '';
  contrasena = '';
  errores = signal<string[]>([]);
  mensajeExito = signal('');

  iniciarSesion(): void {
    this.errores.set([]);
    this.mensajeExito.set('');
    this.identidad.login(this.correo, this.contrasena).subscribe({
      next: (respuesta) => {
        this.sesion.guardar(respuesta.token, respuesta.usuario);
        this.router.navigate(['/']);
      },
      error: (error: HttpErrorResponse) => this.errores.set(leerMensajes(error))
    });
  }

  irARegistro(): void {
    this.errores.set([]);
    this.mensajeExito.set('');
    this.router.navigate(['/login'], { queryParams: { modo: 'registro' } });
  }


  volverAlLogin(cuentaCreada: boolean): void {
    this.mensajeExito.set(cuentaCreada ? '¡Tu cuenta quedó creada! Ya puedes iniciar sesión.' : '');
    this.router.navigate(['/login']);
  }
}