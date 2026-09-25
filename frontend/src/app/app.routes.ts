import { Routes } from '@angular/router';
import {Home} from './shared/presentation/views/home/home';
import {iamGuard} from './iam/infrastructure/iam.guard';

const iamRoutes = () => import('./iam/presentation/iam.routes').then(m => m.iamRoutes);
const groupsRoutes = () => import('./groups/presentation/groups.routes').then(m => m.groupsRoutes);
const baseTitle = 'SynHub'

export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: '**', redirectTo: '/home' }, // Could add a page not found component here?
  { path: 'home', component: Home, title: `${baseTitle} - Home`, canActivate: [iamGuard]},
  { path: 'auth', loadChildren: iamRoutes, title: `${baseTitle}`},
  { path: 'groups', loadChildren: groupsRoutes, title: `${baseTitle} - My Groups`}

];
