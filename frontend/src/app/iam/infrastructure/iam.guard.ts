import { CanActivateFn, Router } from '@angular/router';
import {IamStore} from '../application/iam.store';
import {inject} from '@angular/core';

export const iamGuard: CanActivateFn = (route, state) => {
  const store = inject(IamStore);
  const router = inject(Router);

  console.log(`trying to navigate to ${route.url} with state ${state.url}`);
  if (store.isSignedIn()) return true;
  else {
    router.navigate(['/auth/sign-in']).then();
    return false;
  }
}
