import { Routes } from '@angular/router';

const plans = () => import('./views/plans/plans').then((m) => m.Plans);

export const subscriptionsRoutes: Routes = [{ path: '', loadComponent: plans }];
