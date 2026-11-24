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
});
