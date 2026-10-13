import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { iamInterceptor } from './iam.interceptor';

describe('iamInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let router: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    localStorage.clear();
    router = { navigate: vi.fn().mockResolvedValue(true) };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([iamInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('agrega el token como Bearer', () => {
    localStorage.setItem('token', 'jwt');
    http.get('/api/v1/tasks/1').subscribe();
    expect(backend.expectOne('/api/v1/tasks/1').request.headers.get('Authorization')).toBe('Bearer jwt');
  });

  it('no agrega el token en sign-in ni sign-up', () => {
    localStorage.setItem('token', 'jwt');
    http.post('/api/v1/authentication/sign-in', {}).subscribe();
    http.post('/api/v1/authentication/sign-up', {}).subscribe();
    expect(backend.expectOne('/api/v1/authentication/sign-in').request.headers.has('Authorization')).toBe(false);
    expect(backend.expectOne('/api/v1/authentication/sign-up').request.headers.has('Authorization')).toBe(false);
  });

  it('sin token, o con "undefined"/"null" guardado, no agrega el header', () => {
    http.get('/a').subscribe();
    localStorage.setItem('token', 'undefined');
    http.get('/b').subscribe();
    localStorage.setItem('token', 'null');
    http.get('/c').subscribe();
    for (const url of ['/a', '/b', '/c']) {
      expect(backend.expectOne(url).request.headers.has('Authorization')).toBe(false);
    }
  });

  it.skip('AUDIT SEC-10: un 401 cierra la sesión y redirige al login', () => {
    localStorage.setItem('token', 'vencido');
    http.get('/api/v1/tasks/1').subscribe({ error: () => undefined });
    backend.expectOne('/api/v1/tasks/1').flush({ message: 'expired' }, { status: 401, statusText: 'Unauthorized' });
    expect(localStorage.getItem('token')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/sign-in'], expect.anything());
  });
});
