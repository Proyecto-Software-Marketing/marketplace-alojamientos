import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Publicacion, PublicacionesService } from '../../servicios/publicaciones.service';
import { leerMensajes } from '../../servicios/errores';

@Component({selector:'app-publicaciones', imports:[RouterLink], templateUrl:'./publicaciones.html', styleUrl:'./publicaciones.css'})
export class Publicaciones {
  private servicio = inject(PublicacionesService);
  items = signal<Publicacion[]>([]); errores = signal<string[]>([]);
  mensaje = signal(''); cargando = signal(false); procesando = signal<string | null>(null);
  constructor() { this.cargar(); }
  cargar() {
    this.cargando.set(true); this.errores.set([]);
    this.servicio.misPublicaciones().subscribe({
      next: r => {this.items.set(r.items ?? []);this.cargando.set(false);},
      error: (e:HttpErrorResponse) => {this.errores.set(leerMensajes(e));this.cargando.set(false);}
    });
  }
  enviar(item:Publicacion) {
    if (!confirm(`¿Enviar "${item.titulo}" a revisión?`)) return;
    this.procesando.set(item.id);this.errores.set([]);this.mensaje.set('');
    this.servicio.enviarRevision(item.id).subscribe({
      next: () => {this.procesando.set(null);this.mensaje.set('Publicación enviada a revisión.');this.cargar();},
      error:(e:HttpErrorResponse) => {this.procesando.set(null);this.errores.set(leerMensajes(e));}
    });
  }
}
