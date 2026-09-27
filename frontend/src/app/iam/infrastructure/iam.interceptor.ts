import { HttpInterceptorFn } from '@angular/common/http';

export const iamInterceptor: HttpInterceptorFn = (request, next) => {
  if (
    request.url.includes('/authentication/sign-in') ||
    request.url.includes('/authentication/sign-up')
  ) {
    return next(request);
  }

  const token = localStorage.getItem('token');
  if (!token || token === 'undefined' || token === 'null') {
    return next(request);
  }

  return next(request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  }));
};
