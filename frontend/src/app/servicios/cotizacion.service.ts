import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

// HU-22 cotización desglosada, valores en pesos sin decimales
export interface Cotizacion {
  id: string;
  publicacionId: number;
  fechaLlegada: string;
  fechaSalida: string;
  cantidadHuespedes: number;
  noches: number;
  precioPorNoche: number;
  precioBase: number;
  tarifaLimpieza: number;
  serviciosAdicionales: { nombre: string; costo: number }[];
  porcentajeDescuento: number;
  descuento: number;
  porcentajeTarifaServicio: number;
  tarifaServicio: number;
  total: number;
}

export interface DatosCotizacion {
  publicacionId: number;
  fechaLlegada: string;
  fechaSalida: string;
  cantidadHuespedes: number;
}


@Injectable({ providedIn: 'root' })
export class CotizacionService {

  private http = inject(HttpClient);
  private url = '/api/cotizacion';


  consultarTarifa() {
    return this.http.get<{ porcentaje: number }>(`${this.url}/tarifas/servicio`);
  }

  cambiarTarifa(porcentaje: number | null) {
    return this.http.put<{ porcentaje: number }>(`${this.url}/tarifas/servicio`, { porcentaje });
  }

  // HU-22 solo el huésped puede cotizar
  cotizar(datos: DatosCotizacion) {
    return this.http.post<Cotizacion>(`${this.url}/quotes`, datos);
  }
}