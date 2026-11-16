import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuditoriaService, EventoAuditoria } from '../../servicios/auditoria.service';
import { CotizacionService } from '../../servicios/cotizacion.service';
import { leerMensajes } from '../../servicios/errores';
import { DatosDashboard, FormatoReporte, ReportesService, TipoReporte } from '../../servicios/reportes.service';

// Reportes que el administrador puede exportar
const TIPOS_REPORTE: { valor: TipoReporte; texto: string }[] = [
  { valor: 'USUARIOS', texto: 'Usuarios' },
  { valor: 'PUBLICACIONES', texto: 'Publicaciones' },
  { valor: 'RESERVAS', texto: 'Reservas' },
  { valor: 'INGRESOS', texto: 'Ingresos estimados' },
  { valor: 'DEMANDA', texto: 'Demanda por ciudad' }
];

// Formatos en los que se puede descargar el reporte
const FORMATOS: { valor: FormatoReporte; texto: string }[] = [
  { valor: 'PDF', texto: 'PDF' },
  { valor: 'EXCEL', texto: 'Excel' }
];

// Panel del administrador
@Component({
  selector: 'app-dashboard',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard {

  private reportes = inject(ReportesService);
  private cotizacion = inject(CotizacionService);
  private auditoria = inject(AuditoriaService);

  datos = signal<DatosDashboard | null>(null);
  errores = signal<string[]>([]);

  // HU-41 (CA-114): el administrador ve quien exporto cada reporte y cuando
  exportaciones = signal<EventoAuditoria[]>([]);
  erroresExportaciones = signal<string[]>([]);

  // HU-40: el administrador elige el reporte y el formato y lo descarga
  readonly tiposReporte = TIPOS_REPORTE;
  readonly formatos = FORMATOS;
  tipoReporte: TipoReporte = 'USUARIOS';
  formato: FormatoReporte = 'PDF';
  exportando = signal(false);
  erroresExportar = signal<string[]>([]);
  mensajeExportar = signal('');

  porcentaje: number | null = null;
  porcentajeActual = signal<number | null>(null);
  erroresTarifa = signal<string[]>([]);
  mensajeTarifa = signal('');

  constructor() {
    this.cargar();
    this.cargarTarifa();
    this.cargarExportaciones();
  }


  cargar(): void {
    this.errores.set([]);
    this.reportes.dashboard().subscribe({
      next: (datos) => this.datos.set(datos),
      error: (error: HttpErrorResponse) => this.errores.set(leerMensajes(error))
    });
  }

  // HU-41 (CA-114): pide el registro de las exportaciones de reportes
  cargarExportaciones(): void {
    this.erroresExportaciones.set([]);
    this.auditoria.exportaciones().subscribe({
      next: (respuesta) => this.exportaciones.set(respuesta.items),
      error: (error: HttpErrorResponse) => this.erroresExportaciones.set(leerMensajes(error))
    });
  }

  // HU-40 (CA-111, CA-112): descarga el reporte en PDF o Excel.
  // HU-41 (CA-113): la descarga queda registrada en Auditoria.
  descargarReporte(): void {
    this.erroresExportar.set([]);
    this.mensajeExportar.set('');
    this.exportando.set(true);

    const tipo = this.tipoReporte;
    const formato = this.formato;

    this.reportes.exportar(tipo, formato).subscribe({
      next: (archivo) => {
        this.guardarArchivo(archivo, this.nombreArchivo(tipo, formato));
        this.exportando.set(false);
        this.mensajeExportar.set('Reporte descargado. La exportación quedó registrada en la auditoría.');
        this.cargarExportaciones();
      },
      error: (error: HttpErrorResponse) => {
        this.exportando.set(false);
        this.leerErrorArchivo(error).then((mensajes) => this.erroresExportar.set(mensajes));
      }
    });
  }

  // Guarda el archivo en el computador del administrador
  private guardarArchivo(archivo: Blob, nombre: string): void {
    const url = URL.createObjectURL(archivo);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombre;
    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    URL.revokeObjectURL(url);
  }

  // El nombre del archivo es el mismo que usa el servicio de Reportes
  private nombreArchivo(tipo: TipoReporte, formato: FormatoReporte): string {
    const extension = formato === 'EXCEL' ? 'xlsx' : 'pdf';
    const fecha = new Date().toISOString().slice(0, 10);
    return `reporte-${tipo.toLowerCase()}-${fecha}.${extension}`;
  }

  // Cuando la respuesta es un archivo, el error tambien llega como archivo y hay que leerlo
  private async leerErrorArchivo(error: HttpErrorResponse): Promise<string[]> {
    const cuerpo = error.error;
    if (cuerpo instanceof Blob) {
      try {
        const texto = await cuerpo.text();
        const datos = JSON.parse(texto);
        if (Array.isArray(datos?.mensajes)) {
          return datos.mensajes;
        }
      } catch (fallo) {
        return leerMensajes(error);
      }
    }
    return leerMensajes(error);
  }

  // Muestra la fecha de la exportacion en formato local
  fecha(exportacion: EventoAuditoria): string {
    return new Date(exportacion.fecha).toLocaleString();
  }

  estados(): { nombre: string; cantidad: number }[] {
    const datos = this.datos();
    if (!datos) {
      return [];
    }
    return Object.entries(datos.reservasPorEstado).map(([nombre, cantidad]) => ({ nombre, cantidad }));
  }

  totalReservas(): number {
    return this.estados().reduce((suma, estado) => suma + estado.cantidad, 0);
  }


  anchoEstado(cantidad: number): number {
    const mayor = Math.max(...this.estados().map((estado) => estado.cantidad), 1);
    return Math.round((cantidad / mayor) * 100);
  }

  anchoCiudad(reservas: number): number {
    const ciudades = this.datos()?.ciudadesMayorDemanda ?? [];
    const mayor = Math.max(...ciudades.map((ciudad) => ciudad.reservas), 1);
    return Math.round((reservas / mayor) * 100);
  }


  colorEstado(estado: string): string {
    switch (estado) {
      case 'CONFIRMADA': return 'verde';
      case 'COMPLETADA': return 'azul';
      case 'CANCELADA': return 'rojo';
      default: return 'naranja';
    }
  }


  cargarTarifa(): void {
    this.cotizacion.consultarTarifa().subscribe({
      next: (respuesta) => {
        this.porcentajeActual.set(respuesta.porcentaje);
        this.porcentaje = respuesta.porcentaje;
      },
      error: (error: HttpErrorResponse) => this.erroresTarifa.set(leerMensajes(error))
    });
  }


  guardarTarifa(): void {
    this.erroresTarifa.set([]);
    this.mensajeTarifa.set('');
    this.cotizacion.cambiarTarifa(this.porcentaje).subscribe({
      next: () => {
        this.mensajeTarifa.set(`La tarifa de servicio quedó en ${this.porcentaje} %.`);
        this.cargarTarifa();
      },
      error: (error: HttpErrorResponse) => this.erroresTarifa.set(leerMensajes(error))
    });
  }
}
