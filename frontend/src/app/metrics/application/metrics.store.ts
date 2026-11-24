import { Injectable, inject, signal } from '@angular/core';
import { MetricsApi } from '../infrastructure/metrics.api';
import { ToastStore } from '../../shared/application/toast.store';
import { TaskDistribution } from '../domain/model/task-distribution.model';
import { TaskOverview } from '../domain/model/task-overview.model';
import { RescheduledTasks } from '../domain/model/rescheduled-tasks.model';

@Injectable({ providedIn: 'root' })
export class MetricsStore {
  private readonly metricsApi = inject(MetricsApi);
  private readonly toastStore = inject(ToastStore);

  private readonly distributionSignal = signal<TaskDistribution | null>(null);
  private readonly overviewSignal = signal<TaskOverview | null>(null);
  private readonly rescheduledSignal = signal<RescheduledTasks | null>(null);
  private readonly loadingOverviewSignal = signal(false);
  private readonly loadingDistributionSignal = signal(false);
  private readonly loadingRescheduledSignal = signal(false);

  readonly distribution = this.distributionSignal.asReadonly();
  readonly overview = this.overviewSignal.asReadonly();
  readonly rescheduled = this.rescheduledSignal.asReadonly();
  readonly loadingOverview = this.loadingOverviewSignal.asReadonly();
  readonly loadingDistribution = this.loadingDistributionSignal.asReadonly();
  readonly loadingRescheduled = this.loadingRescheduledSignal.asReadonly();

  loadGroupOverview(groupId: number): void {
    this.loadingOverviewSignal.set(true);
    this.overviewSignal.set(null);
    this.metricsApi.getGroupOverview(groupId).subscribe({
      next: (data) => {
        this.overviewSignal.set(data);
        this.loadingOverviewSignal.set(false);
      },
      error: (err: Error) => {
        this.loadingOverviewSignal.set(false);
        this.toastStore.error(err.message);
      },
    });
  }

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
    this.overviewSignal.set(null);
    this.distributionSignal.set(null);
    this.rescheduledSignal.set(null);
    this.loadingOverviewSignal.set(false);
    this.loadingDistributionSignal.set(false);
    this.loadingRescheduledSignal.set(false);
  }
}
