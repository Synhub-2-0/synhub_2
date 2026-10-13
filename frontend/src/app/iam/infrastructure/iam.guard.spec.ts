import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { iamGuard } from './iam.guard';
import { IamStore } from '../application/iam.store';
import { saveSession } from './session';

describe('iamGuard', () => {
  const route = { url: [] } as unknown as ActivatedRouteSnapshot;
  const state = { url: '/home' } as RouterStateSnapshot;
  const inOneHour = () => new Date(Date.now() + 3_600_000).toISOString();
  let isSignedIn: ReturnType<typeof signal<boolean>>;
  let endSession: ReturnType<typeof vi.fn>;
  let router: { navigate: ReturnType<typeof vi.fn> };

  const runGuard = () => TestBed.runInInjectionContext(() => iamGuard(route, state));

  beforeEach(() => {
    localStorage.clear();
    isSignedIn = signal(false);
    endSession = vi.fn();
    router = { navigate: vi.fn().mockResolvedValue(true) };
    TestBed.configureTestingModule({
      providers: [
        { provide: IamStore, useValue: { isSignedIn, endSession } },
        { provide: Router, useValue: router },
      ],
    });
  });

  it('deja pasar con sesión iniciada y vigente', () => {
    saveSession(5, inOneHour());
    isSignedIn.set(true);
    expect(runGuard()).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('sin sesión redirige al login', () => {
    expect(runGuard()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/auth/sign-in']);
  });

  it('AUDIT SEC-10: una sesión vencida se cierra y redirige al login', () => {
    saveSession(5, new Date(Date.now() - 60_000).toISOString());
    isSignedIn.set(true);
    expect(runGuard()).toBe(false);
    expect(endSession).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/sign-in']);
  });
});
