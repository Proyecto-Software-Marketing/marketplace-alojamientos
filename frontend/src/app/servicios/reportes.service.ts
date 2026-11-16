import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';


export interface DatosDashboard {
  usuarios: number;
  publicacionesActivas: number;
  reservasPorEstado: Record<string, number>;
  ingresosEstimados: number;
  ciudadesMayorDemanda: { ciudad: string; reservas: number }[];
}


@Injectable({ providedIn: 'root' })
export class ReportesService {

  private http = inject(HttpClient);
  private url = '/api/reportes';

  dashboard() {
    return this.http.get<DatosDashboard>(`${this.url}/admin/dashboard`);
  }
}