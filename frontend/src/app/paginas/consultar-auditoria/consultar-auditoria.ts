import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuditoriaService, EventoAuditoria } from '../../servicios/auditoria.service';
import { leerMensajes } from '../../servicios/errores';

@Component({
  selector: 'app-consultar-auditoria',
  imports: [FormsModule, DatePipe],
  templateUrl: './consultar-auditoria.html',
  styleUrl: './consultar-auditoria.css'
})
export class ConsultarAuditoria {
  private auditoria = inject(AuditoriaService);

  usuario = '';
  entidad = '';
  accion = '';
  desde = '';
  hasta = '';

  eventos = signal<EventoAuditoria[]>([]);
  errores = signal<string[]>([]);
  cargando = signal(false);

  constructor() {
    this.buscar();
  }

  buscar() {
    if (this.cargando()) return;

    if (this.desde && this.hasta && this.desde > this.hasta) {
      this.errores.set(['La fecha inicial no puede ser posterior a la fecha final.']);
      return;
    }

    const usuario = this.usuario.trim();

    if (usuario && (!/^\d+$/.test(usuario) || Number(usuario) < 1 || !Number.isSafeInteger(Number(usuario)))) {
      this.errores.set(['El usuario debe ser un numero entero positivo.']);
      return;
    }

    this.cargando.set(true);
    this.errores.set([]);

    this.auditoria.consultar({
      usuario: usuario ? Number(usuario) : undefined,
      entidad: this.entidad.trim(),
      accion: this.accion.trim(),
      desde: this.desde,
      hasta: this.hasta
    }).subscribe({
      next: respuesta => {
        this.eventos.set(respuesta.items);
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.eventos.set([]);
        this.errores.set(leerMensajes(error));
        this.cargando.set(false);
      }
    });
  }

  limpiar() {
    this.usuario = '';
    this.entidad = '';
    this.accion = '';
    this.desde = '';
    this.hasta = '';
    this.buscar();
  }
}