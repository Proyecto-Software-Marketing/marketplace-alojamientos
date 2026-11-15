import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SesionService } from './sesion.service';

// Solo deja entrar si inicio sesion
export const sesionGuard: CanActivateFn = () => {
  return inject(SesionService).estaLogueado() ? true : inject(Router).createUrlTree(['/login']);
};

// Solo deja entrar al administrador
export const adminGuard: CanActivateFn = () => {
  return inject(SesionService).esAdmin() ? true : inject(Router).createUrlTree(['/']);
};