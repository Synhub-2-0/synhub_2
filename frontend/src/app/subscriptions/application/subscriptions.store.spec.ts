import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { SubscriptionsStore } from './subscriptions.store';
import { SubscriptionsApi } from '../infrastructure/subscriptions.api';
import { IamStore } from '../../iam/application/iam.store';
import { ToastStore } from '../../shared/application/toast.store';
import { Subscription } from '../domain/model/subscription.entity';
import { ChangePlanCommand } from '../domain/model/change-plan.command';

const subscription = (plan: 'FREE' | 'PRO' | 'ULTRA' = 'FREE') => new Subscription({
  plan, planName: plan, monthlyPrice: 0, maxLedGroups: plan === 'ULTRA' ? null : 2, activeLedGroups: 1, lockedLedGroups: 0,
  availableSlots: plan === 'ULTRA' ? null : 1, plans: [],
});

describe('SubscriptionsStore', () => {
  let api: Record<'getMySubscription' | 'changePlan', ReturnType<typeof vi.fn>>;
  let isSignedIn: ReturnType<typeof signal<boolean>>;
  let store: SubscriptionsStore;
  let toasts: ToastStore;

  beforeEach(() => {
    api = { getMySubscription: vi.fn(), changePlan: vi.fn() };
    isSignedIn = signal(true);
    TestBed.configureTestingModule({
      providers: [{ provide: SubscriptionsApi, useValue: api }, { provide: IamStore, useValue: { isSignedIn } }],
    });
    store = TestBed.inject(SubscriptionsStore);
    toasts = TestBed.inject(ToastStore);
  });

  it('carga mi plan', () => {
    api.getMySubscription.mockReturnValue(of(subscription('PRO')));
    store.load();
    expect(store.subscription()?.plan).toBe('PRO');
    expect(store.loading()).toBe(false);
  });

  it('si no se puede cargar, queda sin plan y avisa', () => {
    api.getMySubscription.mockReturnValue(throwError(() => new Error('No se pudo cargar tu plan.')));
    store.load();
    expect(store.subscription()).toBeNull();
    expect(toasts.toasts()[0].message).toBe('No se pudo cargar tu plan.');
  });

  it('cambiar de plan actualiza el plan, avisa y llama a onSuccess', () => {
    api.changePlan.mockReturnValue(of(subscription('PRO')));
    const onSuccess = vi.fn();

    store.changePlan(new ChangePlanCommand('PRO'), onSuccess);

    expect(api.changePlan).toHaveBeenCalledTimes(1);
    expect(store.subscription()?.plan).toBe('PRO');
    expect(store.changing()).toBe(false);
    expect(toasts.toasts()[0].message).toContain(subscription('PRO').planName);
    expect(onSuccess).toHaveBeenCalled();
  });

  it('un error al cambiar de plan se muestra, no cambia el plan y no llama a onSuccess', () => {
    api.getMySubscription.mockReturnValue(of(subscription('FREE')));
    store.load();
    api.changePlan.mockReturnValue(throwError(() => new Error('Elige exactamente los grupos que seguirán activos con el nuevo plan.')));
    const onSuccess = vi.fn();

    store.changePlan(new ChangePlanCommand('FREE'), onSuccess);

    expect(store.error()).toBe('Elige exactamente los grupos que seguirán activos con el nuevo plan.');
    expect(store.subscription()?.plan).toBe('FREE');
    expect(onSuccess).not.toHaveBeenCalled();
    expect(store.changing()).toBe(false);
  });

  it('al cerrar sesión se vacía el plan', () => {
    api.getMySubscription.mockReturnValue(of(subscription('PRO')));
    store.load();
    isSignedIn.set(false);
    TestBed.tick();
    expect(store.subscription()).toBeNull();
  });
});
