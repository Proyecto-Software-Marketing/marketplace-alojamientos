import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { IdentidadService } from './servicios/identidad.service';
import { SesionService } from './servicios/sesion.service';

// menu de arriba y la ventana de cada ruta
@Component({
  selector: 'app-root',
    imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {

  sesion = inject(SesionService);
  private identidad = inject(IdentidadService);
  private router = inject(Router);


  cerrarSesion(): void {
    this.identidad.logout().subscribe({
      complete: () => this.salir(),
 
      error: () => this.salir()
    });
  }

  private salir(): void {
    this.sesion.cerrar();
    this.router.navigate(['/login']);
  }
}