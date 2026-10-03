import {Routes} from '@angular/router';

const validations = () => import('./views/validations/validations').then(m => m.Validations);

export const requestsRoutes: Routes = [
  { path: '', loadComponent: validations }
];
