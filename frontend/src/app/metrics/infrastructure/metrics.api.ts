import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MetricsApiEndpoint } from './metrics.api-endpoint';
import { TaskDistribution } from '../domain/model/task-distribution.model';
import { RescheduledTasks } from '../domain/model/rescheduled-tasks.model';

@Injectable({ providedIn: 'root' })
export class MetricsApi {
  private readonly endpoint: MetricsApiEndpoint;

  constructor() {
    const http = inject(HttpClient);
    this.endpoint = new MetricsApiEndpoint(http);
  }

  getGroupDistribution(groupId: number): Observable<TaskDistribution> {
    return this.endpoint.getGroupDistribution(groupId);
  }

  getGroupRescheduled(groupId: number): Observable<RescheduledTasks> {
    return this.endpoint.getGroupRescheduled(groupId);
  }
}
