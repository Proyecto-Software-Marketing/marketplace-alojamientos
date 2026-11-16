import { Routes } from '@angular/router';
import { Inicio } from './paginas/inicio/inicio';
import { Login } from './paginas/login/login';
import { Detalle } from './paginas/detalle/detalle';
import { Perfil } from './paginas/perfil/perfil';
import { adminGuard, sesionGuard } from './servicios/guardias';
import { Disponibilidad } from './paginas/disponibilidad/disponibilidad';
import { Dashboard } from './paginas/dashboard/dashboard';
import { RecuperarContrasena } from './paginas/recuperar-contrasena/recuperar-contrasena';
import { Notificaciones } from './paginas/notificaciones/notificaciones';
import { MisReservas } from './paginas/mis-reservas/mis-reservas';

// Rutas de las ventanas
export const routes: Routes = [
  { path: '', component: Inicio },
  { path: 'login', component: Login },
  { path: 'registro', redirectTo: 'login' },
  { path: 'recuperar-contrasena', component: RecuperarContrasena },
  { path: 'alojamiento/:id', component: Detalle },
  { path: 'disponibilidad', component: Disponibilidad, canActivate: [sesionGuard] },
  { path: 'disponibilidad/:id', component: Disponibilidad, canActivate: [sesionGuard] },
  { path: 'perfil', component: Perfil, canActivate: [sesionGuard] },
  { path: 'notificaciones', component: Notificaciones, canActivate: [sesionGuard] },
  { path: 'mis-reservas', component: MisReservas, canActivate: [sesionGuard] },
  { path: 'dashboard', component: Dashboard, canActivate: [adminGuard] },
  { path: '**', redirectTo: '' }
];