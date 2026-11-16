import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, concatMap } from 'rxjs';
import { AlojamientosService, Publicacion, ServicioAdicional, TipoAlojamiento } from '../../servicios/alojamientos.service';
import { leerMensajes } from '../../servicios/errores';
import { SesionService } from '../../servicios/sesion.service';

type Paso = 'datos' | 'detalles' | 'tarifas' | 'listo';

// Los mismos servicios que el filtro de búsqueda
const CATALOGO_SERVICIOS = ['Wifi', 'Cocina', 'Aire acondicionado', 'Lavadora', 'Aparcamiento', 'Piscina'];

const TIPOS: { valor: TipoAlojamiento; texto: string }[] = [
  { valor: 'APARTAMENTO', texto: 'Apartamento' },
  { valor: 'CASA', texto: 'Casa' },
  { valor: 'HABITACION', texto: 'Habitación' },
  { valor: 'CABANA', texto: 'Cabaña' },
  { valor: 'OTRO', texto: 'Otro' }
];

// HU-07, HU-09 y HU-10 el anfitrión crea una publicación por pasos
@Component({
  selector: 'app-nueva-publicacion',
  imports: [FormsModule, RouterLink, DecimalPipe],
  templateUrl: './nueva-publicacion.html',
  styleUrl: './nueva-publicacion.css'
})
export class NuevaPublicacion {

  private alojamientos = inject(AlojamientosService);
  private sesion = inject(SesionService);

  readonly tipos = TIPOS;
  readonly catalogo = CATALOGO_SERVICIOS;
  readonly pasos: { valor: Paso; texto: string }[] = [
    { valor: 'datos', texto: 'Datos básicos' },
    { valor: 'detalles', texto: 'Servicios, reglas e imágenes' },
    { valor: 'tarifas', texto: 'Tarifas' },
    { valor: 'listo', texto: 'Listo' }
  ];

  esAnfitrion = computed(() => this.sesion.usuario()?.rol === 'ANFITRION');

  paso = signal<Paso>('datos');
  // la publicación que devuelve el backend después de cada paso
  publicacion = signal<Publicacion | null>(null);
  errores = signal<string[]>([]);
  enviando = signal(false);

  // Paso 1 (HU-07)
  titulo = '';
  descripcion = '';
  ciudad = '';
  ubicacionTextual = '';
  tipoAlojamiento: TipoAlojamiento | '' = '';
  capacidad: number | null = null;
  precioPorNoche: number | null = null;

  // Paso 2 (HU-09)
  servicios = signal<string[]>([]);
  reglas = signal<string[]>([]);
  reglaNueva = '';
  imagenes = signal<string[]>([]);
  imagenNueva = '';

  // Paso 3 (HU-10)
  tarifaLimpieza: number | null = 0;
  porcentajeDescuento: number | null = 0;
  nochesMinimas: number | null = null;
  adicionales = signal<ServicioAdicional[]>([]);
  adicionalNombre = '';
  adicionalCosto: number | null = null;

  numeroPaso(paso: Paso): number {
    return this.pasos.findIndex((item) => item.valor === paso) + 1;
  }

  // ---------- Paso 1: datos básicos (CA-021 a CA-024) ----------
  crearBorrador(): void {
    const faltan: string[] = [];
    if (!this.titulo.trim()) faltan.push('el título');
    if (!this.descripcion.trim()) faltan.push('la descripción');
    if (!this.ciudad.trim()) faltan.push('la ciudad');
    if (!this.ubicacionTextual.trim()) faltan.push('la ubicación');
    if (!this.tipoAlojamiento) faltan.push('el tipo de alojamiento');
    if (this.capacidad === null) faltan.push('la capacidad');
    if (this.precioPorNoche === null) faltan.push('el precio por noche');
    if (faltan.length > 0) {
      this.errores.set([`Falta ${faltan.join(', ')}`]);
      return;
    }

    this.enviar(this.alojamientos.crear({
      titulo: this.titulo.trim(),
      descripcion: this.descripcion.trim(),
      ciudad: this.ciudad.trim(),
      ubicacionTextual: this.ubicacionTextual.trim(),
      tipoAlojamiento: this.tipoAlojamiento as TipoAlojamiento,
      capacidad: this.capacidad as number,
      precioPorNoche: this.precioPorNoche as number
    }), (publicacion) => {
      this.publicacion.set(publicacion);
      this.paso.set('detalles');
    });
  }

  // ---------- Paso 2: servicios, reglas e imágenes (CA-026 a CA-028) ----------
  cambiarServicio(servicio: string): void {
    this.servicios.update((lista) =>
      lista.includes(servicio) ? lista.filter((item) => item !== servicio) : [...lista, servicio]);
  }

  agregarRegla(): void {
    const regla = this.reglaNueva.trim();
    if (!regla || this.reglas().includes(regla)) {
      return;
    }
    this.reglas.update((lista) => [...lista, regla]);
    this.reglaNueva = '';
  }

  quitarRegla(indice: number): void {
    this.reglas.update((lista) => lista.filter((_, i) => i !== indice));
  }

  agregarImagen(): void {
    const imagen = this.imagenNueva.trim();
    if (!imagen || this.imagenes().includes(imagen)) {
      return;
    }
    this.imagenes.update((lista) => [...lista, imagen]);
    this.imagenNueva = '';
  }

  quitarImagen(indice: number): void {
    this.imagenes.update((lista) => lista.filter((_, i) => i !== indice));
  }

  // Las tres listas se guardan una tras otra: la última respuesta trae la publicación completa.
  // Si una falla se queda en el paso; volver a enviar no hace daño porque cada lista reemplaza la anterior
  guardarDetalles(): void {
    const id = this.publicacion()!.id;
    // el catálogo define el orden, igual que lo guarda el backend
    const servicios = this.catalogo.filter((servicio) => this.servicios().includes(servicio));
    const peticion = this.alojamientos.guardarServicios(id, servicios).pipe(
      concatMap(() => this.alojamientos.guardarReglas(id, this.reglas())),
      concatMap(() => this.alojamientos.guardarImagenes(id, this.imagenes()))
    );
    this.enviar(peticion, (publicacion) => {
      this.publicacion.set(publicacion);
      this.paso.set('tarifas');
    });
  }

  // ---------- Paso 3: tarifas (CA-029 a CA-031, CA-129) ----------
  agregarAdicional(): void {
    const nombre = this.adicionalNombre.trim();
    if (!nombre || this.adicionalCosto === null) {
      this.errores.set(['Escribe el nombre y el costo del servicio adicional']);
      return;
    }
    this.errores.set([]);
    this.adicionales.update((lista) => [...lista, { nombre, costo: this.adicionalCosto as number }]);
    this.adicionalNombre = '';
    this.adicionalCosto = null;
  }

  quitarAdicional(indice: number): void {
    this.adicionales.update((lista) => lista.filter((_, i) => i !== indice));
  }

  guardarTarifas(): void {
    const porcentaje = this.porcentajeDescuento ?? 0;
    this.enviar(this.alojamientos.guardarTarifas(this.publicacion()!.id, {
      tarifaLimpieza: this.tarifaLimpieza ?? 0,
      porcentajeDescuentoEstanciaLarga: porcentaje,
      // sin descuento no se envían noches mínimas, el backend lo rechazaría
      nochesMinimasDescuento: porcentaje > 0 ? this.nochesMinimas : null,
      serviciosAdicionales: this.adicionales()
    }), (publicacion) => {
      this.publicacion.set(publicacion);
      this.paso.set('listo');
    });
  }

  // Los pasos 2 y 3 son opcionales
  omitir(): void {
    this.errores.set([]);
    this.paso.set(this.paso() === 'detalles' ? 'tarifas' : 'listo');
  }

  crearOtra(): void {
    this.titulo = '';
    this.descripcion = '';
    this.ciudad = '';
    this.ubicacionTextual = '';
    this.tipoAlojamiento = '';
    this.capacidad = null;
    this.precioPorNoche = null;
    this.servicios.set([]);
    this.reglas.set([]);
    this.imagenes.set([]);
    this.tarifaLimpieza = 0;
    this.porcentajeDescuento = 0;
    this.nochesMinimas = null;
    this.adicionales.set([]);
    this.publicacion.set(null);
    this.errores.set([]);
    this.paso.set('datos');
  }

  textoTipo(tipo: string): string {
    return this.tipos.find((item) => item.valor === tipo)?.texto ?? tipo;
  }

  // Envía la petición del paso y muestra los errores del backend (400, 403, 404, 409)
  private enviar<T>(peticion: Observable<T>, alTerminar: (respuesta: T) => void): void {
    this.errores.set([]);
    this.enviando.set(true);
    peticion.subscribe({
      next: (respuesta) => {
        this.enviando.set(false);
        alTerminar(respuesta);
      },
      error: (error: HttpErrorResponse) => {
        this.enviando.set(false);
        this.errores.set(leerMensajes(error));
      }
    });
  }
}
