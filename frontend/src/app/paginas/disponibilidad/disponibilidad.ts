import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DisponibilidadService, TramoCalendario } from '../../servicios/disponibilidad.service';
import { leerMensajes } from '../../servicios/errores';
import { SesionService } from '../../servicios/sesion.service';

// Un cuadro del calendario; fecha null es un hueco antes del día 1
interface Dia {
  fecha: string | null;
  numero: number;
  clase: string;
}

// Periodo armado con los tramos del calendario que tienen el mismo id
interface PeriodoVista {
  id: number;
  fechaInicio: string;
  fechaFin: string;
  tieneReservas: boolean;
  bloqueado: boolean;
}

// Fecha local como texto AAAA-MM-DD, igual que las fechas del backend
function aTexto(fecha: Date): string {
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
}

// HU-17 calendario de disponibilidad de una publicación y HU-16 periodos del anfitrión
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
  private sesion = inject(SesionService);

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

  // HU-16 solo el anfitrión ve el formulario; el backend verifica que sea el dueño
  esAnfitrion = computed(() => this.sesion.usuario()?.rol === 'ANFITRION');

  // Decisión 8: el contrato no tiene GET de periodos, se juntan los tramos por id
  periodos = computed(() => {
    const porId = new Map<number, PeriodoVista>();
    for (const tramo of this.tramos()) {
      const periodo = porId.get(tramo.id);
      if (periodo) {
        periodo.fechaInicio = tramo.fechaInicio < periodo.fechaInicio ? tramo.fechaInicio : periodo.fechaInicio;
        periodo.fechaFin = tramo.fechaFin > periodo.fechaFin ? tramo.fechaFin : periodo.fechaFin;
        periodo.tieneReservas ||= tramo.estado === 'RESERVADO';
        periodo.bloqueado ||= tramo.estado === 'BLOQUEADO';
      } else {
        porId.set(tramo.id, {
          id: tramo.id,
          fechaInicio: tramo.fechaInicio,
          fechaFin: tramo.fechaFin,
          tieneReservas: tramo.estado === 'RESERVADO',
          bloqueado: tramo.estado === 'BLOQUEADO'
        });
      }
    }
    return [...porId.values()].sort((a, b) => a.fechaInicio.localeCompare(b.fechaInicio));
  });

  // Formulario para agregar o editar un periodo
  fechaInicio = '';
  fechaFin = '';
  editando = signal<number | null>(null);
  confirmandoEliminar = signal<number | null>(null);
  erroresPeriodo = signal<string[]>([]);
  mensajeExito = signal('');

  constructor() {
    this.ruta.paramMap.subscribe((parametros) => {
      const id = parametros.get('id');
      this.errores.set([]);
      this.tramos.set([]);
      this.limpiarFormulario();
      this.mensajeExito.set('');
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

  // CA-045 agregar o, si se está editando, guardar los cambios del periodo
  guardar(): void {
    const id = this.idPublicacion();
    if (id === null) {
      return;
    }
    this.erroresPeriodo.set([]);
    this.mensajeExito.set('');
    const datos = { fechaInicio: this.fechaInicio, fechaFin: this.fechaFin };
    const idPeriodo = this.editando();
    const peticion = idPeriodo === null
      ? this.disponibilidad.registrarPeriodo(id, datos)
      : this.disponibilidad.editarPeriodo(id, idPeriodo, datos);

    peticion.subscribe({
      next: () => {
        this.mensajeExito.set(idPeriodo === null ? 'El periodo quedó registrado.' : 'El periodo quedó actualizado.');
        this.limpiarFormulario();
        this.cargar();
      },
      // CA-046 y CA-047: fechas inválidas (400), periodo cruzado o reserva fuera (409)
      error: (error: HttpErrorResponse) => this.erroresPeriodo.set(leerMensajes(error))
    });
  }

  // Decisión 9: si el periodo ya empezó, el calendario lo muestra desde hoy
  editar(periodo: PeriodoVista): void {
    this.erroresPeriodo.set([]);
    this.mensajeExito.set('');
    this.confirmandoEliminar.set(null);
    this.editando.set(periodo.id);
    this.fechaInicio = periodo.fechaInicio;
    this.fechaFin = periodo.fechaFin;
  }

  cancelarEdicion(): void {
    this.erroresPeriodo.set([]);
    this.limpiarFormulario();
  }

  pedirConfirmacion(idPeriodo: number): void {
    this.erroresPeriodo.set([]);
    this.mensajeExito.set('');
    this.confirmandoEliminar.set(idPeriodo);
  }

  // CA-047 no se elimina si tiene reservas confirmadas (409)
  eliminar(idPeriodo: number): void {
    const id = this.idPublicacion();
    if (id === null) {
      return;
    }
    this.disponibilidad.eliminarPeriodo(id, idPeriodo).subscribe({
      complete: () => {
        this.confirmandoEliminar.set(null);
        this.mensajeExito.set('El periodo quedó eliminado.');
        if (this.editando() === idPeriodo) {
          this.limpiarFormulario();
        }
        this.cargar();
      },
      error: (error: HttpErrorResponse) => {
        this.confirmandoEliminar.set(null);
        this.erroresPeriodo.set(leerMensajes(error));
      }
    });
  }

  // 2026-11-20 se muestra como 20 nov 2026
  fechaCorta(texto: string): string {
    const [anio, mes, dia] = texto.split('-').map(Number);
    return new Date(anio, mes - 1, dia).toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  private limpiarFormulario(): void {
    this.editando.set(null);
    this.confirmandoEliminar.set(null);
    this.fechaInicio = '';
    this.fechaFin = '';
  }
}
