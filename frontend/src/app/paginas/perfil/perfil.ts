import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { leerMensajes } from '../../servicios/errores';
import { IdentidadService } from '../../servicios/identidad.service';
import { SesionService } from '../../servicios/sesion.service';

// Ver, actualizar datos y desactivar la cuenta 
@Component({
  selector: 'app-perfil',
  imports: [FormsModule],
  templateUrl: './perfil.html',
  styleUrl: './perfil.css'
})
export class Perfil {

  private identidad = inject(IdentidadService);
  private sesion = inject(SesionService);
  private router = inject(Router);

  nombre = '';
  correo = '';
  rol = signal('');
  errores = signal<string[]>([]);
  mensajeExito = signal('');


  confirmando = signal(false);
  erroresCuenta = signal<string[]>([]);

  constructor() {

    this.identidad.consultarPerfil().subscribe({
      next: (usuario) => {
        this.nombre = usuario.nombre;
        this.correo = usuario.correo;
        this.rol.set(usuario.rol);
      },
      error: (error: HttpErrorResponse) => this.errores.set(leerMensajes(error))
    });
  }

  guardar(): void {
    this.errores.set([]);
    this.mensajeExito.set('');
    this.identidad.actualizarPerfil(this.nombre, this.correo).subscribe({
      next: (usuario) => {
        this.sesion.actualizarUsuario(usuario);
        this.mensajeExito.set('Tus datos quedaron actualizados.');
      },
      error: (error: HttpErrorResponse) => this.errores.set(leerMensajes(error))
    });
  }

  
  pedirConfirmacion(): void {
    this.erroresCuenta.set([]);
    this.confirmando.set(true);
  }

  desactivar(): void {
    this.identidad.desactivarCuenta().subscribe({
      complete: () => {
        this.sesion.cerrar();
        this.router.navigate(['/login']);
      },
      
      error: (error: HttpErrorResponse) => {
        this.confirmando.set(false);
        this.erroresCuenta.set(leerMensajes(error));
      }
    });
  }
}