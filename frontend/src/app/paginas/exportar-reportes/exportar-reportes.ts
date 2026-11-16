import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuditoriaService, EventoAuditoria } from '../../servicios/auditoria.service';
import { leerMensajes } from '../../servicios/errores';
import { FormatoReporte, ReportesService, TipoReporte } from '../../servicios/reportes.service';

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

// Ventana de exportacion de reportes del administrador.
// HU-40 (CA-111, CA-112): se descarga el reporte en PDF o Excel.
// HU-41 (CA-113, CA-114): cada descarga queda registrada y se ve en la auditoria.
@Component({
  selector: 'app-exportar-reportes',
  imports: [FormsModule],
  templateUrl: './exportar-reportes.html',
  styleUrl: './exportar-reportes.css'
})
export class ExportarReportes {

  private reportes = inject(ReportesService);
  private auditoria = inject(AuditoriaService);

  readonly tiposReporte = TIPOS_REPORTE;
  readonly formatos = FORMATOS;

  tipoReporte: TipoReporte = 'USUARIOS';
  formato: FormatoReporte = 'PDF';

  exportando = signal(false);
  erroresExportar = signal<string[]>([]);
  mensajeExportar = signal('');

  // HU-41 (CA-114): el registro de las exportaciones
  exportaciones = signal<EventoAuditoria[]>([]);
  erroresExportaciones = signal<string[]>([]);

  constructor() {
    this.cargarExportaciones();
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

  // HU-41 (CA-114): pide el registro de las exportaciones de reportes
  cargarExportaciones(): void {
    this.erroresExportaciones.set([]);
    this.auditoria.exportaciones().subscribe({
      next: (respuesta) => this.exportaciones.set(respuesta.items),
      error: (error: HttpErrorResponse) => this.erroresExportaciones.set(leerMensajes(error))
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
}
