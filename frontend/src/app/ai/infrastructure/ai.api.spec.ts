import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AiApi } from './ai.api';
import { environment } from '../../../environments/environment';

describe('AiApi (HTTP)', () => {
  const url = `${environment.platformProviderApiBaseUrl}${environment.platformProviderAiEndpointPath}`;
  let api: AiApi;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(AiApi);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('classify envía la fecha límite en ISO', () => {
    api.classifyTask({ title: 'Login', description: 'd', dueDate: new Date('2026-12-01T10:00:00Z') }).subscribe();
    const req = backend.expectOne({ method: 'POST', url: `${url}/classify` });
    expect(req.request.body).toEqual({ title: 'Login', description: 'd', dueDate: '2026-12-01T10:00:00.000Z' });
    req.flush({ labels: [], urgency: 'LOW', difficulty: 1, context: 'GENERAL', rationale: '' });
  });

  it('classify sin fecha no envía dueDate', () => {
    api.classifyTask({ title: 'Login', description: '' }).subscribe();
    const req = backend.expectOne(`${url}/classify`);
    expect(req.request.body.dueDate).toBeUndefined();
    req.flush({});
  });

  it('el informe devuelve solo el texto', () => {
    let report = '';
    api.getGroupReport(10).subscribe((r) => (report = r));
    backend.expectOne({ method: 'GET', url: `${url}/groups/10/report` }).flush({ report: '## Resumen' });
    expect(report).toBe('## Resumen');
  });
});
