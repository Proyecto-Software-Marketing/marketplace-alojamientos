import { HttpErrorResponse } from '@angular/common/http';

// los mensajes de error con nuestro formato {"mensajes": [...]}
export function leerMensajes(error: HttpErrorResponse): string[] {
  if (error.error?.mensajes) {
    return error.error.mensajes;
  }
  if (error.status === 0 || error.status >= 500) {
    return ['No se pudo conectar con el servidor. ¿Está encendido el servicio?'];
  }
  return ['Ocurrió un error inesperado'];
}