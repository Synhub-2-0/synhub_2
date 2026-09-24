import {Routes} from '@angular/router';

const signInForm = () =>
  import('./views/sign-in-page/sign-in-page').then(m => m.SignInPage);
const signUpForm = () =>
  import('./views/sign-up-page/sign-up-page').then(m => m.SignUpPage);

export const iamRoutes: Routes = [
  { path: 'sign-in', loadComponent: signInForm},
  { path: 'sign-up', loadComponent: signUpForm}
];
