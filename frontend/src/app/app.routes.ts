import { Routes } from '@angular/router';
import {Home} from './shared/presentation/views/home/home';
import {iamGuard} from './iam/infrastructure/iam.guard';
import {PageNotFound} from './shared/presentation/views/page-not-found/page-not-found';

const iamRoutes = () => import('./iam/presentation/iam.routes').then(m => m.iamRoutes);
const groupsRoutes = () => import('./groups/presentation/groups.routes').then(m => m.groupsRoutes);
const tasksRoutes = () => import('./tasks/presentation/tasks.routes').then(m => m.tasksRoutes);
const baseTitle = 'SynHub'

export const routes: Routes = [
  { path: 'auth', loadChildren: iamRoutes, title: `${baseTitle}`},
  { path: 'home', component: Home, title: `${baseTitle} - Home`, canActivate: [iamGuard]},
  { path: 'groups', loadChildren: groupsRoutes, title: `${baseTitle} - My Groups`, canActivate: [iamGuard]},
  { path: 'tasks', loadChildren: tasksRoutes, title: `${baseTitle} - Tasks`, canActivate: [iamGuard]},
  { path: '', redirectTo: '/auth/sign-in', pathMatch: 'full' },
  { path: '**', component: PageNotFound, title: `${baseTitle} - Page not found` }
];
