import { CanActivateFn, Router } from '@angular/router';
import { IamStore } from '../application/iam.store';
import { inject } from '@angular/core';
import { hasActiveSession } from './session';

/** Deja pasar solo con una sesión iniciada y vigente; una sesión vencida se cierra (AUDIT SEC-10). */
export const iamGuard: CanActivateFn = () => {
  const store = inject(IamStore);
  const router = inject(Router);
  if (store.isSignedIn() && hasActiveSession()) return true;
  if (store.isSignedIn()) store.endSession();
  router.navigate(['/auth/sign-in']).then();
  return false;
};
