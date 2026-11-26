import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MetricsApiEndpoint } from './metrics.api-endpoint';
import { TaskDistribution } from '../domain/model/task-distribution.model';
import { TaskOverview } from '../domain/model/task-overview.model';
import { RescheduledTasks } from '../domain/model/rescheduled-tasks.model';
import { AvgCompletionTime } from '../domain/model/avg-completion-time.model';

@Injectable({ providedIn: 'root' })
export class MetricsApi {
  private readonly endpoint: MetricsApiEndpoint;

  constructor() {
    const http = inject(HttpClient);
    this.endpoint = new MetricsApiEndpoint(http);
  }

  getGroupOverview(groupId: number): Observable<TaskOverview> {
    return this.endpoint.getGroupOverview(groupId);
  }

  getGroupDistribution(groupId: number): Observable<TaskDistribution> {
    return this.endpoint.getGroupDistribution(groupId);
  }

  getGroupRescheduled(groupId: number): Observable<RescheduledTasks> {
    return this.endpoint.getGroupRescheduled(groupId);
  }

  getGroupAvgCompletionTime(groupId: number): Observable<AvgCompletionTime> {
    return this.endpoint.getGroupAvgCompletionTime(groupId);
  }

  getMemberOverview(groupId: number, userId: number): Observable<TaskOverview> {
    return this.endpoint.getMemberOverview(groupId, userId);
  }

  getMemberRescheduled(groupId: number, userId: number): Observable<RescheduledTasks> {
    return this.endpoint.getMemberRescheduled(groupId, userId);
  }

  getMemberAvgCompletionTime(groupId: number, userId: number): Observable<AvgCompletionTime> {
    return this.endpoint.getMemberAvgCompletionTime(groupId, userId);
  }
}
