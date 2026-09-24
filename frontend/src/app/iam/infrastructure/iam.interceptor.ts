import {HttpInterceptorFn} from '@angular/common/http';
import {IamStore} from '../application/iam.store';
import {inject} from '@angular/core';

export const iamInterceptor: HttpInterceptorFn = (
  request, next) => {
  const store = inject(IamStore);
  const token = store.currentToken();
  const handledRequest = token
    ? request.clone({headers: request.headers
        .set('Authorization', `Bearer ${token}`)})
    : request;
  return next(handledRequest);
}
