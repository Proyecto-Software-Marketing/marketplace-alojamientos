import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuditoriaService, EventoAuditoria } from '../../servicios/auditoria.service';
import { CotizacionService } from '../../servicios/cotizacion.service';
import { leerMensajes } from '../../servicios/errores';
import { DatosDashboard, ReportesService } from '../../servicios/reportes.service';

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
