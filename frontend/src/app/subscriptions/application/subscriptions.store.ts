import { Injectable, effect, inject, signal } from '@angular/core';
import { IamStore } from '../../iam/application/iam.store';
import { ToastStore } from '../../shared/application/toast.store';
import { Subscription } from '../domain/model/subscription.entity';
import { ChangePlanCommand } from '../domain/model/change-plan.command';
import { SubscriptionsApi } from '../infrastructure/subscriptions.api';

@Injectable({ providedIn: 'root' })
export class SubscriptionsStore {
  private readonly subscriptionsApi = inject(SubscriptionsApi);
  private readonly toastStore = inject(ToastStore);
  private readonly iamStore = inject(IamStore);

  private readonly subscriptionSignal = signal<Subscription | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly changingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly subscription = this.subscriptionSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly changing = this.changingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  constructor() {
    // Root stores outlive the session: drop the previous user's plan as soon as the session ends.
    effect(() => {
      if (!this.iamStore.isSignedIn()) this.reset();
    });
  }

  reset(): void {
    this.subscriptionSignal.set(null);
    this.errorSignal.set(null);
  }

  load(): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.subscriptionsApi.getMySubscription().subscribe({
      next: (subscription) => {
        this.subscriptionSignal.set(subscription);
        this.loadingSignal.set(false);
      },
      error: (err) => {
        this.subscriptionSignal.set(null);
        this.setError(err.message);
        this.loadingSignal.set(false);
      },
    });
  }

  /** Simulated payment: confirming the plan is enough. Going down needs the groups that stay active. */
  changePlan(command: ChangePlanCommand, onSuccess?: () => void): void {
    this.changingSignal.set(true);
    this.errorSignal.set(null);
    this.subscriptionsApi.changePlan(command).subscribe({
      next: (subscription) => {
        this.subscriptionSignal.set(subscription);
        this.changingSignal.set(false);
        this.toastStore.success(`Plan ${subscription.planName} activado.`);
        onSuccess?.();
      },
      error: (err) => {
        this.setError(err.message);
        this.changingSignal.set(false);
      },
    });
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  private setError(message: string): void {
    this.errorSignal.set(message);
    this.toastStore.error(message);
  }
}
