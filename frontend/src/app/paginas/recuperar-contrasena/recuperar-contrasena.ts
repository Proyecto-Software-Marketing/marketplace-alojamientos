import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { leerMensajes } from '../../servicios/errores';
import { IdentidadService } from '../../servicios/identidad.service';
import { NotificacionesService } from '../../servicios/notificaciones.service';
import { SesionService } from '../../servicios/sesion.service';

// HU-04 pedir un token con el correo y cambiar la contraseña con ese token
@Component({
  selector: 'app-recuperar-contrasena',
  imports: [FormsModule, RouterLink],
  templateUrl: './recuperar-contrasena.html',
  styleUrl: './recuperar-contrasena.css'
})
export class RecuperarContrasena {

  private identidad = inject(IdentidadService);
  private notificaciones = inject(NotificacionesService);
  private sesion = inject(SesionService);

  // correo: pedir el token, token: escribir la nueva contraseña, listo: contraseña cambiada
  paso = signal<'correo' | 'token' | 'listo'>('correo');

  correo = '';
  token = '';
  nuevaContrasena = '';
  confirmacion = '';
  errores = signal<string[]>([]);
  mensajeBuzon = signal('');
  tokenVencido = signal(false);

  // CA-010 el sistema genera el token y lo deja en el buzón simulado
  enviarCorreo(): void {
    this.errores.set([]);
    this.identidad.recuperarContrasena(this.correo.trim()).subscribe({
      complete: () => {
        this.paso.set('token');
        this.revisarBuzon();
      },
      error: (error: HttpErrorResponse) => this.errores.set(leerMensajes(error))
    });
  }

  revisarBuzon(): void {
    this.notificaciones.buzonRecuperacion(this.correo.trim()).subscribe({
      next: (lista) => {
        const mensaje = lista.items[0]?.mensaje ?? '';
        // el token es un UUID dentro del mensaje, se llena solo pero se puede editar
        const encontrado = mensaje.match(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i);
        if (encontrado) {
          this.token = encontrado[0];
        }
        this.mensajeBuzon.set(mensaje);
      },
      error: (error: HttpErrorResponse) => this.errores.set(leerMensajes(error))
    });
  }

  // CA-011 con un token válido se cambia la contraseña
  restablecer(): void {
    this.errores.set([]);
    this.tokenVencido.set(false);
    if (this.nuevaContrasena !== this.confirmacion) {
      this.errores.set(['Las contraseñas no coinciden']);
      return;
    }
    this.identidad.restablecerContrasena(this.token.trim(), this.nuevaContrasena).subscribe({
      complete: () => {
        // el backend cierra las sesiones abiertas, entonces se borra la que haya en el navegador
        this.sesion.cerrar();
        this.paso.set('listo');
      },
      error: (error: HttpErrorResponse) => {
        this.errores.set(leerMensajes(error));
        // CA-012 token vencido o ya usado
        this.tokenVencido.set(error.status === 410);
      }
    });
  }

  pedirOtroToken(): void {
    this.errores.set([]);
    this.tokenVencido.set(false);
    this.token = '';
    this.nuevaContrasena = '';
    this.confirmacion = '';
    this.mensajeBuzon.set('');
    this.paso.set('correo');
  }
}
