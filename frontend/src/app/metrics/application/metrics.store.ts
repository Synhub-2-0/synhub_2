import { Injectable, inject, signal } from '@angular/core';
import { MetricsApi } from '../infrastructure/metrics.api';
import { ToastStore } from '../../shared/application/toast.store';
import { TaskDistribution } from '../domain/model/task-distribution.model';
import { RescheduledTasks } from '../domain/model/rescheduled-tasks.model';

@Injectable({ providedIn: 'root' })
export class MetricsStore {
  private readonly metricsApi = inject(MetricsApi);
  private readonly toastStore = inject(ToastStore);

  private readonly distributionSignal = signal<TaskDistribution | null>(null);
  private readonly rescheduledSignal = signal<RescheduledTasks | null>(null);
  private readonly loadingDistributionSignal = signal(false);
  private readonly loadingRescheduledSignal = signal(false);

  readonly distribution = this.distributionSignal.asReadonly();
  readonly rescheduled = this.rescheduledSignal.asReadonly();
  readonly loadingDistribution = this.loadingDistributionSignal.asReadonly();
  readonly loadingRescheduled = this.loadingRescheduledSignal.asReadonly();

  loadGroupDistribution(groupId: number): void {
    this.loadingDistributionSignal.set(true);
    this.distributionSignal.set(null);
    this.metricsApi.getGroupDistribution(groupId).subscribe({
      next: (data) => {
        this.distributionSignal.set(data);
        this.loadingDistributionSignal.set(false);
      },
      error: (err: Error) => {
        this.loadingDistributionSignal.set(false);
        this.toastStore.error(err.message);
      },
    });
  }

  loadGroupRescheduled(groupId: number): void {
    this.loadingRescheduledSignal.set(true);
    this.rescheduledSignal.set(null);
    this.metricsApi.getGroupRescheduled(groupId).subscribe({
      next: (data) => {
        this.rescheduledSignal.set(data);
        this.loadingRescheduledSignal.set(false);
      },
      error: (err: Error) => {
        this.loadingRescheduledSignal.set(false);
        this.toastStore.error(err.message);
      },
    });
  }

  reset(): void {
    this.distributionSignal.set(null);
    this.rescheduledSignal.set(null);
  }
}
