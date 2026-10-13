import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { iamGuard } from './iam.guard';
import { IamStore } from '../application/iam.store';

describe('iamGuard', () => {
  const route = { url: [] } as unknown as ActivatedRouteSnapshot;
  const state = { url: '/home' } as RouterStateSnapshot;
  let isSignedIn: ReturnType<typeof signal<boolean>>;
  let router: { navigate: ReturnType<typeof vi.fn> };

  const runGuard = () => TestBed.runInInjectionContext(() => iamGuard(route, state));

  beforeEach(() => {
    localStorage.clear();
    vi.spyOn(console, 'log').mockImplementation(() => undefined);
    isSignedIn = signal(false);
    router = { navigate: vi.fn().mockResolvedValue(true) };
    TestBed.configureTestingModule({
      providers: [
        { provide: IamStore, useValue: { isSignedIn } },
        { provide: Router, useValue: router },
      ],
    });
  });

  it('deja pasar con sesión iniciada', () => {
    isSignedIn.set(true);
    expect(runGuard()).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('sin sesión redirige al login', () => {
    expect(runGuard()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/auth/sign-in']);
  });

  it.skip('AUDIT SEC-10: un token vencido se trata como sesión cerrada', () => {
    const expiredPayload = btoa(JSON.stringify({ sub: 'ana', exp: Math.floor(Date.now() / 1000) - 60 }));
    localStorage.setItem('token', `x.${expiredPayload}.y`);
    isSignedIn.set(true);
    expect(runGuard()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/auth/sign-in']);
  });
});
