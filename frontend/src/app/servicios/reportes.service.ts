import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';


export interface DatosDashboard {
  usuarios: number;
  publicacionesActivas: number;
  reservasPorEstado: Record<string, number>;
  ingresosEstimados: number;
  ciudadesMayorDemanda: { ciudad: string; reservas: number }[];
}

// Reportes de gestion que el administrador puede exportar
export type TipoReporte = 'USUARIOS' | 'PUBLICACIONES' | 'RESERVAS' | 'INGRESOS' | 'DEMANDA';

// Formatos en los que se puede descargar el reporte
export type FormatoReporte = 'PDF' | 'EXCEL';


@Injectable({ providedIn: 'root' })
export class ReportesService {

  private http = inject(HttpClient);
  private url = '/api/reportes';

  dashboard() {
    return this.http.get<DatosDashboard>(`${this.url}/admin/dashboard`);
  }

  // HU-40 (CA-111, CA-112): el archivo del reporte se descarga como PDF o Excel.
  // HU-41 (CA-113): cada descarga queda registrada en Auditoria.
  exportar(tipoReporte: TipoReporte, formato: FormatoReporte) {
    return this.http.post(`${this.url}/admin/reports/export`, { tipoReporte, formato },
      { responseType: 'blob' });
  }
}
