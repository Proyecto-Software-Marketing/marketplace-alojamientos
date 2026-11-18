import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { PublicacionInput, PublicacionesService } from '../../servicios/publicaciones.service';
import { leerMensajes } from '../../servicios/errores';

@Component({selector:'app-formulario-publicacion',imports:[FormsModule,RouterLink],templateUrl:'./formulario-publicacion.html',styleUrl:'./formulario-publicacion.css'})
export class FormularioPublicacion {
  private servicio=inject(PublicacionesService);private ruta=inject(ActivatedRoute);private router=inject(Router);
  id=this.ruta.snapshot.paramMap.get('id');
  datos:PublicacionInput={titulo:'',descripcion:'',ciudad:'',ubicacionTextual:'',tipoAlojamiento:'APARTAMENTO',capacidad:1,precioPorNoche:1};
  errores=signal<string[]>([]);cargando=signal(false);guardando=signal(false);
  constructor(){if(this.id){this.cargando.set(true);this.servicio.consultar(this.id).subscribe({next:p=>{this.datos={titulo:p.titulo,descripcion:p.descripcion,ciudad:p.ciudad,ubicacionTextual:p.ubicacionTextual,tipoAlojamiento:p.tipoAlojamiento,capacidad:p.capacidad,precioPorNoche:p.precioPorNoche};this.cargando.set(false);},error:(e:HttpErrorResponse)=>{this.errores.set(leerMensajes(e));this.cargando.set(false);}});}}
  guardar(){
    if(this.guardando()||this.cargando())return;
    const capacidad = Number(this.datos.capacidad);
    const precio = Number(this.datos.precioPorNoche);

    if (
      !this.datos.titulo.trim() ||
      this.datos.titulo.length > 120 ||
      !this.datos.descripcion.trim() ||
      !this.datos.ciudad.trim() ||
      !this.datos.ubicacionTextual.trim() ||
      !Number.isInteger(capacidad) ||
      capacidad < 1 ||
      capacidad > 50 ||
      !Number.isInteger(precio) ||
      precio < 1 ||
      precio > 999999999
    ) {
      this.errores.set([
        'Revisa los campos: titulo maximo 120 caracteres, capacidad entre 1 y 50 y precio entero entre 1 y 999999999 COP.'
      ]);
      return;
    }
    this.errores.set([]);this.guardando.set(true);
    const datos={...this.datos,capacidad:Number(this.datos.capacidad),precioPorNoche:Number(this.datos.precioPorNoche)};
    const peticion=this.id?this.servicio.editar(this.id,datos):this.servicio.crear(datos);
    peticion.subscribe({next:()=>{this.guardando.set(false);this.router.navigate(['/mis-publicaciones']);},error:(e:HttpErrorResponse)=>{this.guardando.set(false);this.errores.set(leerMensajes(e));}});
  }
}
