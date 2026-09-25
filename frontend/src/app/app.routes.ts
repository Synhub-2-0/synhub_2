import { Routes } from '@angular/router';
import {Home} from './shared/presentation/views/home/home';
import {iamGuard} from './iam/infrastructure/iam.guard';

const iamRoutes = () => import('./iam/presentation/iam.routes').then(m => m.iamRoutes);
const baseTitle = 'SynHub'

export const routes: Routes = [
  { path: 'home', component: Home, title: `${baseTitle} - Home`, canActivate: [iamGuard]},
  { path: 'auth', loadChildren: iamRoutes, title: `${baseTitle}`}
];
