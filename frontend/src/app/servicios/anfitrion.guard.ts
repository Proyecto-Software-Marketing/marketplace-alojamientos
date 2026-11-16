import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SesionService } from './sesion.service';
export const anfitrionGuard: CanActivateFn = () => {
  const sesion = inject(SesionService);
  return sesion.usuario()?.rol === 'ANFITRION' ? true : inject(Router).createUrlTree(['/']);
};
export const editorGuard: CanActivateFn = () => {
  const rol = inject(SesionService).usuario()?.rol;
  return rol === 'ANFITRION' || rol === 'ADMINISTRADOR' ? true : inject(Router).createUrlTree(['/']);
};
