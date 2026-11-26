import { HttpClient, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { MetricsApiEndpoint } from './metrics.api-endpoint';

describe('MetricsApiEndpoint', () => {
  let endpoint: MetricsApiEndpoint;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    endpoint = new MetricsApiEndpoint(TestBed.inject(HttpClient));
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('obtiene el resumen de estado del grupo y transforma la respuesta REST', () => {
    let result: unknown;
    endpoint.getGroupOverview(42).subscribe((overview) => (result = overview));

    const request = http.expectOne(
      `${environment.platformProviderApiBaseUrl}${environment.platformProviderMetricsEndpointPath}/groups/42/overview`,
    );
    expect(request.request.method).toBe('GET');
    request.flush({ type: 'TASK_OVERVIEW', value: 3, details: { DONE: 1, IN_PROGRESS: 2 } });

    expect(result).toEqual({ type: 'TASK_OVERVIEW', total: 3, details: { DONE: 1, IN_PROGRESS: 2 } });
  });

  it('obtiene el tiempo promedio de cierre del grupo en días', () => {
    let result: unknown;
    endpoint.getGroupAvgCompletionTime(42).subscribe((avg) => (result = avg));

    const request = http.expectOne(
      `${environment.platformProviderApiBaseUrl}${environment.platformProviderMetricsEndpointPath}/groups/42/avg-completion-time`,
    );
    request.flush({ type: 'AVG_COMPLETION_TIME', value: 2.5, details: { completedTasks: 4 } });

    expect(result).toEqual({ type: 'AVG_COMPLETION_TIME', days: 2.5, completedTasks: 4 });
  });

  it('obtiene las métricas de un integrante dentro del grupo', () => {
    endpoint.getMemberRescheduled(42, 7).subscribe();

    const request = http.expectOne(
      `${environment.platformProviderApiBaseUrl}${environment.platformProviderMetricsEndpointPath}/groups/42/members/7/rescheduled`,
    );
    expect(request.request.method).toBe('GET');
    request.flush({ type: 'RESCHEDULED_TASKS_MEMBER', value: 1, details: { total: 3, rescheduled: 1 }, rescheduledMemberIds: [7] });
  });
});
