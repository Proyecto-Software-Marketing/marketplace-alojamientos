import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DisponibilidadService, TramoCalendario } from '../../servicios/disponibilidad.service';
import { leerMensajes } from '../../servicios/errores';

// Un cuadro del calendario; fecha null es un hueco antes del día 1
interface Dia {
  fecha: string | null;
  numero: number;
  clase: string;
}

// Fecha local como texto AAAA-MM-DD, igual que las fechas del backend
function aTexto(fecha: Date): string {
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

// HU-17 calendario de disponibilidad de una publicación
@Component({
  selector: 'app-disponibilidad',
  imports: [FormsModule, RouterLink],
  templateUrl: './disponibilidad.html',
  styleUrl: './disponibilidad.css'
})
export class Disponibilidad {

  private disponibilidad = inject(DisponibilidadService);
  private router = inject(Router);
  private ruta = inject(ActivatedRoute);

  diasSemana = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

  // id de la publicación de la ruta /disponibilidad/:id, o null si no viene
  idPublicacion = signal<number | null>(null);
  idEscrito = '';
  tramos = signal<TramoCalendario[]>([]);
  cargando = signal(false);
  errores = signal<string[]>([]);

  // primer día del mes que se está viendo
  mes = signal(new Date(new Date().getFullYear(), new Date().getMonth(), 1));

  tituloMes = computed(() => {
    const texto = this.mes().toLocaleString('es-CO', { month: 'long', year: 'numeric' });
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  // CA-048 cada día toma el estado del tramo que lo contiene
  dias = computed(() => {
    const primero = this.mes();
    const hoy = aTexto(new Date());
    const cantidad = new Date(primero.getFullYear(), primero.getMonth() + 1, 0).getDate();
    // la semana empieza el lunes: getDay() da 0 al domingo
    const huecos = (primero.getDay() + 6) % 7;
    const dias: Dia[] = [];

    for (let i = 0; i < huecos; i++) {
      dias.push({ fecha: null, numero: 0, clase: 'hueco' });
    }
    for (let numero = 1; numero <= cantidad; numero++) {
      const fecha = aTexto(new Date(primero.getFullYear(), primero.getMonth(), numero));
      // fechaFin es el día de salida: esa noche ya no hace parte del tramo
      const tramo = this.tramos().find((t) => t.fechaInicio <= fecha && fecha < t.fechaFin);
      let clase = tramo ? tramo.estado.toLowerCase() : 'sin-periodo';
      if (fecha < hoy) {
        clase += ' pasado';
      }
      dias.push({ fecha, numero, clase });
    }
    return dias;
  });

  constructor() {
    this.ruta.paramMap.subscribe((parametros) => {
      const id = parametros.get('id');
      this.errores.set([]);
      this.tramos.set([]);
      this.idPublicacion.set(id ? Number(id) : null);
      if (id) {
        this.cargar();
      }
    });
  }

  // Sin publicación en la ruta: se escribe el id y se abre su calendario
  abrir(): void {
    const id = Number(this.idEscrito);
    if (!Number.isInteger(id) || id <= 0) {
      this.errores.set(['Escribe el número de la publicación']);
      return;
    }
    this.router.navigate(['/disponibilidad', id]);
  }

  cargar(): void {
    const id = this.idPublicacion();
    if (id === null) {
      return;
    }
    this.cargando.set(true);
    this.disponibilidad.calendario(id).subscribe({
      next: (calendario) => {
        this.tramos.set(calendario.periodos);
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errores.set(leerMensajes(error));
        this.cargando.set(false);
      }
    });
  }

  cambiarMes(cambio: number): void {
    const actual = this.mes();
    this.mes.set(new Date(actual.getFullYear(), actual.getMonth() + cambio, 1));
  }
}
