import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';


// Evento de auditoria que devuelve el servicio de Auditoria
export interface EventoAuditoria {
  id: number;
  usuarioId: number | null;
  entidad: string;
  entidadId: number | null;
  accion: string;
  resultado: string;
  detalle: string | null;
  fecha: string;
}


// Consulta la auditoria (HU-41, CA-114)
@Injectable({ providedIn: 'root' })
export class AuditoriaService {

  private http = inject(HttpClient);
  private url = '/api/auditoria';

  // Los exports de reportes quedan registrados con la accion EXPORTAR_REPORTE
  exportaciones() {
    return this.http.get<{ items: EventoAuditoria[] }>(`${this.url}/admin/audit-events`, {
      params: { entidad: 'REPORTE', accion: 'EXPORTAR_REPORTE' }
    });
  }
}
