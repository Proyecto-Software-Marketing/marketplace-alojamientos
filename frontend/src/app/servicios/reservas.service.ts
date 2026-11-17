import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

// Estado en el que puede estar una reserva
export type EstadoReserva = 'CONFIRMADA' | 'COMPLETADA' | 'CANCELADA' | 'EN_REVISION_MANUAL';

// Una reserva tal como la devuelve el servicio Reservas
export interface Reserva {
  id: number;
  codigoReserva: string;
  publicacionId: number;
  huespedId: number;
  fechaLlegada: string;
  fechaSalida: string;
  cantidadHuespedes: number;
  montoTotal: number;
  estado: EstadoReserva;
  creadaEn: string;
}

// Lista de reservas del historial
export interface ListaReservas {
  items: Reserva[];
}

// Respuesta de la cancelación de una reserva
export interface Cancelacion {
  reservaId: number;
  canceladaPor: number;
  fecha: string;
  motivo: string;
  valorDevolucionSimulada: number;
  estadoFinal: EstadoReserva;
}

// Llamadas al servicio Reservas
@Injectable({ providedIn: 'root' })
export class ReservasService {

  private http = inject(HttpClient);
  private url = '/api/reservas/bookings';

  // HU-26: historial de reservas. El huésped ve las suyas y el anfitrión las de sus publicaciones
  listar(estado?: EstadoReserva) {
    const parametros = estado ? new HttpParams().set('estado', estado) : undefined;
    return this.http.get<ListaReservas>(this.url, { params: parametros });
  }

  // HU-26: detalle de una reserva
  consultar(id: number) {
    return this.http.get<Reserva>(`${this.url}/${id}`);
  }

  // HU-28: cancela la reserva con su motivo y devuelve la devolución simulada
  cancelar(id: number, motivo: string) {
    return this.http.post<Cancelacion>(`${this.url}/${id}/cancel`, { motivo });
  }

  // HU-23: confirma la reserva a partir de la cotización. Un reintento con la misma
  // Idempotency-Key devuelve la misma reserva en lugar de crear otra
  crearReserva(cotizacionId: string, metodoPago: string, idempotencyKey: string) {
    return this.http.post<Reserva>(this.url, { cotizacionId, metodoPago },
      { headers: { 'Idempotency-Key': idempotencyKey } });
  }
}
