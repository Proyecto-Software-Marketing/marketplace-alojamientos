import { Routes } from '@angular/router';
import { Inicio } from './paginas/inicio/inicio';
import { Login } from './paginas/login/login';
import { Detalle } from './paginas/detalle/detalle';
import { Perfil } from './paginas/perfil/perfil';
import { adminGuard, sesionGuard } from './servicios/guardias';
import { Dashboard } from './paginas/dashboard/dashboard';
import { RecuperarContrasena } from './paginas/recuperar-contrasena/recuperar-contrasena';

import { anfitrionGuard, editorGuard } from './servicios/anfitrion.guard';
import { Publicaciones } from './paginas/publicaciones/publicaciones';
import { FormularioPublicacion } from './paginas/formulario-publicacion/formulario-publicacion';
import { Aprobaciones } from './paginas/aprobaciones/aprobaciones';

// Rutas de las ventanas
export const routes: Routes = [
  { path: '', component: Inicio },
  { path: 'login', component: Login },
  { path: 'registro', redirectTo: 'login' },
  { path: 'recuperar-contrasena', component: RecuperarContrasena },
  { path: 'alojamiento/:id', component: Detalle },
  { path: 'perfil', component: Perfil, canActivate: [sesionGuard] },
  { path: 'dashboard', component: Dashboard, canActivate: [adminGuard] },
  { path: 'mis-publicaciones', component: Publicaciones, canActivate: [anfitrionGuard] },
  { path: 'publicaciones/nueva', component: FormularioPublicacion, canActivate: [anfitrionGuard] },
  { path: 'publicaciones/:id/editar', component: FormularioPublicacion, canActivate: [editorGuard] },
  { path: 'aprobaciones', component: Aprobaciones, canActivate: [adminGuard] },
  { path: '**', redirectTo: '' }
];