import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, Injector } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { IamStore } from '../application/iam.store';

const isApiRequest = (url: string) => url.startsWith(environment.platformProviderApiBaseUrl);
const isAuthenticationRequest = (url: string) => url.includes('/authentication/');

/**
 * Sesión por cookie HttpOnly (HUM-04):
 * - las peticiones a la API viajan con credenciales para que el navegador envíe la cookie;
 * - un 401 fuera de /authentication cierra la sesión local y lleva al login (AUDIT SEC-10).
 */
export const iamInterceptor: HttpInterceptorFn = (request, next) => {
  const injector = inject(Injector);
  const router = inject(Router);
  const outgoing = isApiRequest(request.url) ? request.clone({ withCredentials: true }) : request;

  return next(outgoing).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !isAuthenticationRequest(request.url)) {
        // IamStore depende de HttpClient: se resuelve aquí, no al construir el interceptor.
        injector.get(IamStore).endSession();
        router.navigate(['/auth/sign-in'], { queryParams: { expired: 1 } }).then();
      }
      return throwError(() => error);
    }),
  );
};
