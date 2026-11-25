import { HttpClient } from '@angular/common/http';
import { catchError, map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TaskDistribution } from '../domain/model/task-distribution.model';
import { TaskOverview } from '../domain/model/task-overview.model';
import { RescheduledTasks } from '../domain/model/rescheduled-tasks.model';
import { AvgCompletionTime } from '../domain/model/avg-completion-time.model';
import {
  AvgCompletionTimeResource,
  TaskDistributionResource,
  TaskOverviewResource,
  RescheduledTasksResource,
} from './metrics.response';
import { MetricsAssembler } from './metrics.assembler';

const metricsBaseUrl = `${environment.platformProviderApiBaseUrl}${environment.platformProviderMetricsEndpointPath}`;

export class MetricsApiEndpoint {
  private readonly assembler = new MetricsAssembler();

  constructor(private readonly http: HttpClient) {}

  /** GET /api/v1/metrics/groups/{groupId}/overview */
  getGroupOverview(groupId: number): Observable<TaskOverview> {
    return this.http
      .get<TaskOverviewResource>(`${metricsBaseUrl}/groups/${groupId}/overview`)
      .pipe(
        map((resource) => this.assembler.toTaskOverview(resource)),
        catchError(this.handleError(`Failed to fetch overview for group ${groupId}`)),
      );
  }

  /** GET /api/v1/metrics/groups/{groupId}/distribution */
  getGroupDistribution(groupId: number): Observable<TaskDistribution> {
    return this.http
      .get<TaskDistributionResource>(`${metricsBaseUrl}/groups/${groupId}/distribution`)
      .pipe(
        map((resource) => this.assembler.toTaskDistribution(resource)),
        catchError(this.handleError(`Failed to fetch distribution for group ${groupId}`)),
      );
  }

  /** GET /api/v1/metrics/groups/{groupId}/rescheduled */
  getGroupRescheduled(groupId: number): Observable<RescheduledTasks> {
    return this.http
      .get<RescheduledTasksResource>(`${metricsBaseUrl}/groups/${groupId}/rescheduled`)
      .pipe(
        map((resource) => this.assembler.toRescheduledTasks(resource)),
        catchError(this.handleError(`Failed to fetch rescheduled tasks for group ${groupId}`)),
      );
  }

  /** GET /api/v1/metrics/groups/{groupId}/avg-completion-time */
  getGroupAvgCompletionTime(groupId: number): Observable<AvgCompletionTime> {
    return this.http
      .get<AvgCompletionTimeResource>(`${metricsBaseUrl}/groups/${groupId}/avg-completion-time`)
      .pipe(
        map((resource) => this.assembler.toAvgCompletionTime(resource)),
        catchError(this.handleError(`Failed to fetch average completion time for group ${groupId}`)),
      );
  }

  /** GET /api/v1/metrics/groups/{groupId}/members/{userId}/overview (líder o propio integrante) */
  getMemberOverview(groupId: number, userId: number): Observable<TaskOverview> {
    return this.http
      .get<TaskOverviewResource>(`${metricsBaseUrl}/groups/${groupId}/members/${userId}/overview`)
      .pipe(
        map((resource) => this.assembler.toTaskOverview(resource)),
        catchError(this.handleError(`Failed to fetch overview for member ${userId}`)),
      );
  }

  /** GET /api/v1/metrics/groups/{groupId}/members/{userId}/rescheduled */
  getMemberRescheduled(groupId: number, userId: number): Observable<RescheduledTasks> {
    return this.http
      .get<RescheduledTasksResource>(`${metricsBaseUrl}/groups/${groupId}/members/${userId}/rescheduled`)
      .pipe(
        map((resource) => this.assembler.toRescheduledTasks(resource)),
        catchError(this.handleError(`Failed to fetch rescheduled tasks for member ${userId}`)),
      );
  }

  /** GET /api/v1/metrics/groups/{groupId}/members/{userId}/avg-completion-time */
  getMemberAvgCompletionTime(groupId: number, userId: number): Observable<AvgCompletionTime> {
    return this.http
      .get<AvgCompletionTimeResource>(`${metricsBaseUrl}/groups/${groupId}/members/${userId}/avg-completion-time`)
      .pipe(
        map((resource) => this.assembler.toAvgCompletionTime(resource)),
        catchError(this.handleError(`Failed to fetch average completion time for member ${userId}`)),
      );
  }

  private handleError(operation: string) {
    return (error: { status?: number; error?: { message?: string } }): Observable<never> => {
      let msg = operation;
      if (error.status === 403) msg = `${operation}: 403`;
      else if (error.status === 404) msg = `${operation}: 404`;
      else if (error.status) msg = `${operation}: ${error.status}`;
      return new Observable<never>((obs) => obs.error(new Error(msg)));
    };
  }
}
