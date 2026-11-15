import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { leerMensajes } from '../../servicios/errores';
import { IdentidadService } from '../../servicios/identidad.service';


@Component({
  selector: 'app-registro',
  imports: [FormsModule],
  templateUrl: './registro.html',
  styleUrl: './registro.css'
})
export class Registro {

  private identidad = inject(IdentidadService);


  volver = output<boolean>();

  nombre = '';
  correo = '';
  contrasena = '';
  rol = 'HUESPED';
  errores = signal<string[]>([]);

  registrar(): void {
    this.errores.set([]);
    const datos = { nombre: this.nombre, correo: this.correo, contrasena: this.contrasena, rol: this.rol };
    this.identidad.registrar(datos).subscribe({
      next: () => this.volver.emit(true),
      error: (error: HttpErrorResponse) => this.errores.set(leerMensajes(error))
    });
  }
}