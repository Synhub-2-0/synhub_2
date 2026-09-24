import {Routes} from '@angular/router';

const signInPage = () =>
  import('./views/sign-in-page/sign-in-page').then(m => m.SignInPage);
const signUpPage = () =>
  import('./views/sign-up-page/sign-up-page').then(m => m.SignUpPage);

export const iamRoutes: Routes = [
  { path: 'sign-in', loadComponent: signInPage},
  { path: 'sign-up', loadComponent: signUpPage}
];
