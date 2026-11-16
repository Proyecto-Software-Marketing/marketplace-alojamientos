import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';


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
}