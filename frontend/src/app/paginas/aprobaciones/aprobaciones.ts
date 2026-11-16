import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Publicacion, PublicacionesService } from '../../servicios/publicaciones.service';
import { leerMensajes } from '../../servicios/errores';
import { RouterLink } from '@angular/router';
@Component({selector:'app-aprobaciones',imports:[RouterLink],templateUrl:'./aprobaciones.html',styleUrl:'./aprobaciones.css'})
export class Aprobaciones {
 private servicio=inject(PublicacionesService);
 items=signal<Publicacion[]>([]);errores=signal<string[]>([]);mensaje=signal('');cargando=signal(false);procesando=signal<string|null>(null);
 constructor(){this.cargar();}
 cargar(){this.cargando.set(true);this.errores.set([]);this.servicio.pendientes().subscribe({next:r=>{this.items.set(r.items??[]);this.cargando.set(false);},error:(e:HttpErrorResponse)=>{this.errores.set(leerMensajes(e));this.cargando.set(false);}});}
 aprobar(item:Publicacion){if(!confirm(`¿Aprobar "${item.titulo}"?`))return;this.ejecutar(item.id,'aprobada',()=>this.servicio.aprobar(item.id));}
 rechazar(item:Publicacion){const motivo=prompt('Indica el motivo del rechazo:');if(motivo===null)return;if(!motivo.trim()){this.errores.set(['Debes indicar el motivo del rechazo.']);return;}this.ejecutar(item.id,'rechazada',()=>this.servicio.rechazar(item.id,motivo.trim()));}
 private ejecutar(id:string,accion:string,operacion:()=>ReturnType<PublicacionesService['aprobar']>){this.procesando.set(id);this.errores.set([]);this.mensaje.set('');operacion().subscribe({next:()=>{this.procesando.set(null);this.mensaje.set(`Publicación ${accion} correctamente.`);this.cargar();},error:(e:HttpErrorResponse)=>{this.procesando.set(null);this.errores.set(leerMensajes(e));}});}
}
