import { Routes } from '@angular/router';
import { Inicio } from './paginas/inicio/inicio';
import { Login } from './paginas/login/login';
import { Detalle } from './paginas/detalle/detalle';
import { Perfil } from './paginas/perfil/perfil';
import { adminGuard, sesionGuard } from './servicios/guardias';
import { Dashboard } from './paginas/dashboard/dashboard';

// Rutas de las ventanas
export const routes: Routes = [
  { path: '', component: Inicio },
  { path: 'login', component: Login },
  { path: 'registro', redirectTo: 'login' },
  { path: 'alojamiento/:id', component: Detalle },
  { path: 'perfil', component: Perfil, canActivate: [sesionGuard] },
  { path: 'dashboard', component: Dashboard, canActivate: [adminGuard] },
  { path: '**', redirectTo: '' }
];