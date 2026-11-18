import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { concatMap } from 'rxjs';
import { AlojamientosService, DatosPublicacion, PoliticaCancelacion } from '../../servicios/alojamientos.service';
import { leerMensajes } from '../../servicios/errores';
import { SesionService } from '../../servicios/sesion.service';

// HU-08 editar los datos básicos de una publicación (dueño o administrador)
// HU-09 también se pueden completar servicios, reglas e imágenes
@Component({
  selector: 'app-editar-publicacion',
  imports: [FormsModule],
  templateUrl: './editar-publicacion.html',
  styleUrl: './editar-publicacion.css'
})
export class EditarPublicacion {

  private alojamientos = inject(AlojamientosService);
  private ruta = inject(ActivatedRoute);
  private router = inject(Router);
  private sesion = inject(SesionService);

  // id que viene en la ruta /publicaciones/:id/editar
  id = Number(this.ruta.snapshot.paramMap.get('id'));

  readonly tipos = [
    { valor: 'APARTAMENTO', texto: 'Apartamento' },
    { valor: 'CASA', texto: 'Casa' },
    { valor: 'HABITACION', texto: 'Habitación' },
    { valor: 'CABANA', texto: 'Cabaña' },
    { valor: 'OTRO', texto: 'Otro' }
  ];

  // mismo catálogo que en Nueva publicación
  readonly catalogo = ['Wifi', 'Cocina', 'Aire acondicionado', 'Lavadora', 'Aparcamiento', 'Piscina'];

  datos: DatosPublicacion = {
    titulo: '',
    descripcion: '',
    ciudad: '',
    ubicacionTextual: '',
    tipoAlojamiento: 'APARTAMENTO',
    capacidad: 1,
    precioPorNoche: 1
  };

  // listas de HU-09
  servicios = signal<string[]>([]);
  reglas = signal<string[]>([]);
  imagenes = signal<string[]>([]);
  nuevaRegla = '';
  nuevaImagen = '';

  errores = signal<string[]>([]);
  cargando = signal(true);
  guardando = signal(false);
  guardado = signal(false);

  erroresDetalles = signal<string[]>([]);
  guardandoDetalles = signal(false);
  detallesGuardados = signal(false);

  politicas = signal<PoliticaCancelacion[]>([]);
  politicaId: number | null = null;
  politicaGuardada = signal(false);
  erroresPolitica = signal<string[]>([]);

  constructor() {
    // se cargan los datos actuales para llenar el formulario
    this.alojamientos.consultar(this.id).subscribe({
      next: (p) => {
        this.datos = {
          titulo: p.titulo,
          descripcion: p.descripcion,
          ciudad: p.ciudad,
          ubicacionTextual: p.ubicacionTextual,
          tipoAlojamiento: p.tipoAlojamiento,
          capacidad: p.capacidad,
          precioPorNoche: p.precioPorNoche
        };
        this.servicios.set(p.servicios);
        this.reglas.set(p.reglas);
        this.imagenes.set(p.imagenes);
        this.politicaId = p.politicaCancelacionId;
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errores.set(leerMensajes(error));
        this.cargando.set(false);
      }
    });
    this.alojamientos.politicas().subscribe({
      next: (respuesta) => this.politicas.set(respuesta.items),
      error: (error: HttpErrorResponse) => this.erroresPolitica.set(leerMensajes(error))
    });
  }

  guardar(): void {
    this.errores.set([]);
    this.guardado.set(false);
    this.guardando.set(true);
    this.alojamientos.editar(this.id, this.datos).subscribe({
      next: () => {
        this.guardando.set(false);
        this.guardado.set(true);
      },
      error: (error: HttpErrorResponse) => {
        this.guardando.set(false);
        this.errores.set(leerMensajes(error));
      }
    });
  }

  // ---------- HU-09 servicios, reglas e imágenes ----------

  cambiarServicio(servicio: string, marcado: boolean): void {
    if (marcado) {
      this.servicios.update((lista) => [...lista, servicio]);
    } else {
      this.servicios.update((lista) => lista.filter((s) => s !== servicio));
    }
  }

  agregarRegla(): void {
    const regla = this.nuevaRegla.trim();
    if (regla) {
      this.reglas.update((lista) => [...lista, regla]);
      this.nuevaRegla = '';
    }
  }

  quitarRegla(regla: string): void {
    this.reglas.update((lista) => lista.filter((r) => r !== regla));
  }

  agregarImagen(): void {
    const imagen = this.nuevaImagen.trim();
    if (imagen) {
      this.imagenes.update((lista) => [...lista, imagen]);
      this.nuevaImagen = '';
    }
  }

  quitarImagen(imagen: string): void {
    this.imagenes.update((lista) => lista.filter((i) => i !== imagen));
  }

  // se guardan las tres listas una después de la otra
  guardarDetalles(): void {
    this.erroresDetalles.set([]);
    this.detallesGuardados.set(false);
    this.guardandoDetalles.set(true);
    this.alojamientos.guardarServicios(this.id, this.servicios()).pipe(
      concatMap(() => this.alojamientos.guardarReglas(this.id, this.reglas())),
      concatMap(() => this.alojamientos.guardarImagenes(this.id, this.imagenes()))
    ).subscribe({
      next: () => {
        this.guardandoDetalles.set(false);
        this.detallesGuardados.set(true);
      },
      error: (error: HttpErrorResponse) => {
        this.guardandoDetalles.set(false);
        this.erroresDetalles.set(leerMensajes(error));
      }
    });
  }
    guardarPolitica(): void {
    if (this.politicaId === null) {
      return;
    }
    this.erroresPolitica.set([]);
    this.politicaGuardada.set(false);
    this.alojamientos.asociarPolitica(this.id, this.politicaId).subscribe({
      next: () => this.politicaGuardada.set(true),
      error: (error: HttpErrorResponse) => this.erroresPolitica.set(leerMensajes(error))
    });
  }

  // el anfitrión vuelve a sus publicaciones y el administrador a aprobaciones
  volver(): void {
    const ruta = this.sesion.usuario()?.rol === 'ADMINISTRADOR' ? '/aprobaciones' : '/mis-publicaciones';
    this.router.navigate([ruta]);
  }
}