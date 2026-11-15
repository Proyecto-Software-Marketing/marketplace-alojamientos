import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { SesionService } from './sesion.service';


export const tokenInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const token = inject(SesionService).token();
  if (token) {
    peticion = peticion.clone({ setHeaders: { Authorization: 'Bearer ' + token } });
  }
  return siguiente(peticion);
};