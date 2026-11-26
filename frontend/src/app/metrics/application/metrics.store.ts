import { Injectable, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { MetricsApi } from '../infrastructure/metrics.api';
import { ToastStore } from '../../shared/application/toast.store';
import { TaskDistribution } from '../domain/model/task-distribution.model';
import { TaskOverview } from '../domain/model/task-overview.model';
import { RescheduledTasks } from '../domain/model/rescheduled-tasks.model';
import { AvgCompletionTime } from '../domain/model/avg-completion-time.model';

/** Métricas de un integrante dentro de un grupo (US-012). */
export interface MemberMetrics {
  overview: TaskOverview;
  rescheduled: RescheduledTasks;
  avgCompletion: AvgCompletionTime;
}

@Injectable({ providedIn: 'root' })
export class MetricsStore {
  private readonly metricsApi = inject(MetricsApi);
  private readonly toastStore = inject(ToastStore);

  private readonly distributionSignal = signal<TaskDistribution | null>(null);
  private readonly overviewSignal = signal<TaskOverview | null>(null);
  private readonly rescheduledSignal = signal<RescheduledTasks | null>(null);
  private readonly avgCompletionSignal = signal<AvgCompletionTime | null>(null);
  private readonly memberSignal = signal<MemberMetrics | null>(null);
  private readonly memberUserIdSignal = signal<number | null>(null);
  private readonly loadingOverviewSignal = signal(false);
  private readonly loadingDistributionSignal = signal(false);
  private readonly loadingRescheduledSignal = signal(false);
  private readonly loadingAvgCompletionSignal = signal(false);
  private readonly loadingMemberSignal = signal(false);

  readonly distribution = this.distributionSignal.asReadonly();
  readonly overview = this.overviewSignal.asReadonly();
  readonly rescheduled = this.rescheduledSignal.asReadonly();
  readonly avgCompletion = this.avgCompletionSignal.asReadonly();
  readonly member = this.memberSignal.asReadonly();
  readonly memberUserId = this.memberUserIdSignal.asReadonly();
  readonly loadingOverview = this.loadingOverviewSignal.asReadonly();
  readonly loadingDistribution = this.loadingDistributionSignal.asReadonly();
  readonly loadingRescheduled = this.loadingRescheduledSignal.asReadonly();
  readonly loadingAvgCompletion = this.loadingAvgCompletionSignal.asReadonly();
  readonly loadingMember = this.loadingMemberSignal.asReadonly();

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

  loadGroupAvgCompletion(groupId: number): void {
    this.loadingAvgCompletionSignal.set(true);
    this.avgCompletionSignal.set(null);
    this.metricsApi.getGroupAvgCompletionTime(groupId).subscribe({
      next: (data) => {
        this.avgCompletionSignal.set(data);
        this.loadingAvgCompletionSignal.set(false);
      },
      error: (err: Error) => {
        this.loadingAvgCompletionSignal.set(false);
        this.toastStore.error(err.message);
      },
    });
  }

  /** Carga las métricas de un integrante. Una respuesta tardía de otro integrante se descarta. */
  loadMember(groupId: number, userId: number): void {
    this.memberUserIdSignal.set(userId);
    this.loadingMemberSignal.set(true);
    this.memberSignal.set(null);
    forkJoin({
      overview: this.metricsApi.getMemberOverview(groupId, userId),
      rescheduled: this.metricsApi.getMemberRescheduled(groupId, userId),
      avgCompletion: this.metricsApi.getMemberAvgCompletionTime(groupId, userId),
    }).subscribe({
      next: (data) => {
        if (this.memberUserIdSignal() !== userId) return;
        this.memberSignal.set(data);
        this.loadingMemberSignal.set(false);
      },
      error: (err: Error) => {
        if (this.memberUserIdSignal() !== userId) return;
        this.loadingMemberSignal.set(false);
        this.toastStore.error(err.message);
      },
    });
  }

  reset(): void {
    this.overviewSignal.set(null);
    this.distributionSignal.set(null);
    this.rescheduledSignal.set(null);
    this.avgCompletionSignal.set(null);
    this.memberSignal.set(null);
    this.memberUserIdSignal.set(null);
    this.loadingOverviewSignal.set(false);
    this.loadingDistributionSignal.set(false);
    this.loadingRescheduledSignal.set(false);
    this.loadingAvgCompletionSignal.set(false);
    this.loadingMemberSignal.set(false);
  }
}
