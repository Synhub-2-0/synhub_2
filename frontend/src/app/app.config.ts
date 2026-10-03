import {ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners} from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import {provideHttpClient, withFetch, withInterceptors} from '@angular/common/http';
import {iamInterceptor} from './iam/infrastructure/iam.interceptor';
import {IamStore} from './iam/application/iam.store';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withFetch(), withInterceptors([iamInterceptor])),
    provideAppInitializer(() => {
      const iamStore = inject(IamStore);
      return iamStore.restoreSession();
    })
  ]
};
