import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

export type TipoAlojamiento = 'APARTAMENTO' | 'CASA' | 'HABITACION' | 'CABANA' | 'OTRO';

// Servicio que se cobra una vez por estancia (HU-10)
export interface ServicioAdicional {
  nombre: string;
  costo: number;
}

// Una publicación tal como la devuelve el servicio Alojamientos
export interface Publicacion {
  id: number;
  anfitrionId: number;
  titulo: string;
  descripcion: string;
  ciudad: string;
  ubicacionTextual: string;
  tipoAlojamiento: TipoAlojamiento;
  capacidad: number;
  precioPorNoche: number;
  tarifaLimpieza: number;
  porcentajeDescuentoEstanciaLarga: number;
  nochesMinimasDescuento: number | null;
  serviciosAdicionales: ServicioAdicional[];
  servicios: string[];
  reglas: string[];
  imagenes: string[];
  estado: string;
  motivoRechazo: string | null;
  politicaCancelacionId: number | null;
  motivoBloqueo: string | null;
}
export interface PoliticaCancelacion {
  id: number;
  nombre: string;
  descripcion: string;
  porcentajeDevolucion: number;
  permiteLiberacionPeriodo: boolean;
}
// HU-07 datos básicos para crear el borrador
export interface DatosPublicacion {
  titulo: string;
  descripcion: string;
  ciudad: string;
  ubicacionTextual: string;
  tipoAlojamiento: TipoAlojamiento;
  capacidad: number;
  precioPorNoche: number;
}

// HU-10 configuración de precios
export interface DatosTarifas {
  tarifaLimpieza: number;
  porcentajeDescuentoEstanciaLarga: number;
  nochesMinimasDescuento: number | null;
  serviciosAdicionales: ServicioAdicional[];
}

// Llamadas al servicio Alojamientos
@Injectable({ providedIn: 'root' })
export class AlojamientosService {

  private http = inject(HttpClient);
  private url = '/api/alojamientos/listings';
  private urlAdmin = '/api/alojamientos/admin/listings';

  // HU-07 la publicación queda en BORRADOR
  crear(datos: DatosPublicacion) {
    return this.http.post<Publicacion>(this.url, datos);
  }

  // HU-09 cada lista reemplaza la anterior completa
  guardarServicios(id: number, servicios: string[]) {
    return this.http.put<Publicacion>(`${this.url}/${id}/amenities`, { servicios });
  }

  guardarReglas(id: number, reglas: string[]) {
    return this.http.put<Publicacion>(`${this.url}/${id}/rules`, { reglas });
  }

  guardarImagenes(id: number, imagenes: string[]) {
    return this.http.put<Publicacion>(`${this.url}/${id}/images`, { imagenes });
  }

  // HU-10 tarifas, descuento por estancia larga y servicios adicionales
  guardarTarifas(id: number, datos: DatosTarifas) {
    return this.http.put<Publicacion>(`${this.url}/${id}/pricing`, datos);
  }
  // HU-15 las publicaciones del anfitrión que inició sesión
  misPublicaciones() {
    return this.http.get<{ items: Publicacion[] }>(`${this.url}/mine`);
  }

  // HU-08 una publicación (la ve el dueño o el administrador)
  consultar(id: number) {
    return this.http.get<Publicacion>(`${this.url}/${id}`);
  }

  // HU-08 editar los datos básicos
  editar(id: number, datos: DatosPublicacion) {
    return this.http.put<Publicacion>(`${this.url}/${id}`, datos);
  }

  // HU-12 pasar de BORRADOR a PENDIENTE_REVISION
  enviarARevision(id: number) {
    return this.http.post<Publicacion>(`${this.url}/${id}/submit`, {});
  }

  // HU-13 el administrador ve las pendientes de revisión
  pendientes() {
    return this.http.get<{ items: Publicacion[] }>(`${this.urlAdmin}`, {
      params: { estado: 'PENDIENTE_REVISION' }
    });
  }

  // HU-13 aprobar: queda ACTIVA
  aprobar(id: number) {
    return this.http.post<Publicacion>(`${this.urlAdmin}/${id}/approve`, {});
  }

  // HU-13 rechazar con motivo: vuelve a BORRADOR
  rechazar(id: number, motivo: string) {
    return this.http.post<Publicacion>(`${this.urlAdmin}/${id}/reject`, { motivo });
  }
  
  // HU-11 políticas disponibles y asociar una a la publicación
  politicas() {
    return this.http.get<{ items: PoliticaCancelacion[] }>('/api/alojamientos/cancellation-policies');
  }

  asociarPolitica(id: number, politicaId: number) {
    return this.http.put<Publicacion>(`${this.url}/${id}/cancellation-policy`, { politicaId });
  }
    pausar(id: number) {
    return this.http.post<Publicacion>(`${this.url}/${id}/pause`, {});
  }

  reactivar(id: number) {
    return this.http.post<Publicacion>(`${this.url}/${id}/resume`, {});
  }
    activas() {
    return this.http.get<{ items: Publicacion[] }>(`${this.urlAdmin}`, {
      params: { estado: 'ACTIVA' }
    });
  }

  bloquear(id: number, motivo: string) {
    return this.http.post<Publicacion>(`${this.urlAdmin}/${id}/block`, { motivo });
  }
}
