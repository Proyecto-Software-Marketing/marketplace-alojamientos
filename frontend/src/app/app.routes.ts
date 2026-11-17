import { Routes } from '@angular/router';
import { Inicio } from './paginas/inicio/inicio';
import { Login } from './paginas/login/login';
import { Detalle } from './paginas/detalle/detalle';
import { Cotizacion } from './paginas/cotizacion/cotizacion';
import { Perfil } from './paginas/perfil/perfil';
import { adminGuard, sesionGuard } from './servicios/guardias';
import { Disponibilidad } from './paginas/disponibilidad/disponibilidad';
import { NuevaPublicacion } from './paginas/nueva-publicacion/nueva-publicacion';
import { Dashboard } from './paginas/dashboard/dashboard';
import { RecuperarContrasena } from './paginas/recuperar-contrasena/recuperar-contrasena';
import { Notificaciones } from './paginas/notificaciones/notificaciones';
import { MisReservas } from './paginas/mis-reservas/mis-reservas';
import { ExportarReportes } from './paginas/exportar-reportes/exportar-reportes';
import { Calificar } from './paginas/calificar/calificar';
import { Favoritos } from './paginas/favoritos/favoritos';

// Rutas de las ventanas
export const routes: Routes = [
  { path: '', component: Inicio },
  { path: 'login', component: Login },
  { path: 'registro', redirectTo: 'login' },
  { path: 'recuperar-contrasena', component: RecuperarContrasena },
  { path: 'alojamiento/:id', component: Detalle },
  { path: 'cotizacion', component: Cotizacion, canActivate: [sesionGuard] },
  { path: 'disponibilidad', component: Disponibilidad, canActivate: [sesionGuard] },
  { path: 'disponibilidad/:id', component: Disponibilidad, canActivate: [sesionGuard] },
  { path: 'publicaciones/nueva', component: NuevaPublicacion, canActivate: [sesionGuard] },
  { path: 'perfil', component: Perfil, canActivate: [sesionGuard] },
  { path: 'notificaciones', component: Notificaciones, canActivate: [sesionGuard] },
  { path: 'mis-reservas', component: MisReservas, canActivate: [sesionGuard] },
  { path: 'calificar/:idReserva', component: Calificar, canActivate: [sesionGuard] },
  { path: 'dashboard', component: Dashboard, canActivate: [adminGuard] },
  { path: 'exportar-reportes', component: ExportarReportes, canActivate: [adminGuard] },
  { path: 'favoritos', component: Favoritos, canActivate: [sesionGuard] },
  { path: '**', redirectTo: '' }
];