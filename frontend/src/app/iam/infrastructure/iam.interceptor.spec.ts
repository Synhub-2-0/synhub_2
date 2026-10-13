import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { iamInterceptor } from './iam.interceptor';
import { IamStore } from '../application/iam.store';
import { environment } from '../../../environments/environment';

describe('iamInterceptor', () => {
  const api = environment.platformProviderApiBaseUrl;
  let http: HttpClient;
  let backend: HttpTestingController;
  let router: { navigate: ReturnType<typeof vi.fn> };
  let store: { endSession: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    localStorage.clear();
    router = { navigate: vi.fn().mockResolvedValue(true) };
    store = { endSession: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([iamInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
        { provide: IamStore, useValue: store },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('las peticiones a la API viajan con la cookie de sesión y sin header Authorization', () => {
    localStorage.setItem('token', 'jwt-de-una-version-anterior');
    http.get(`${api}/tasks/1`).subscribe();
    const request = backend.expectOne(`${api}/tasks/1`).request;
    expect(request.withCredentials).toBe(true);
    expect(request.headers.has('Authorization')).toBe(false);
  });

  it('las peticiones a otros dominios no llevan credenciales', () => {
    http.get('https://img.logo.dev/x.png').subscribe();
    expect(backend.expectOne('https://img.logo.dev/x.png').request.withCredentials).toBe(false);
  });

  it('AUDIT SEC-10: un 401 cierra la sesión y redirige al login', () => {
    let failed = false;
    http.get(`${api}/tasks/1`).subscribe({ error: () => (failed = true) });
    backend.expectOne(`${api}/tasks/1`).flush({ message: 'expired' }, { status: 401, statusText: 'Unauthorized' });
    expect(failed).toBe(true);
    expect(store.endSession).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/sign-in'], { queryParams: { expired: 1 } });
  });

  it('un 401 del login (credenciales inválidas) no redirige', () => {
    http.post(`${api}/authentication/sign-in`, {}).subscribe({ error: () => undefined });
    backend.expectOne(`${api}/authentication/sign-in`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(store.endSession).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('otros errores no cierran la sesión', () => {
    http.get(`${api}/tasks/1`).subscribe({ error: () => undefined });
    backend.expectOne(`${api}/tasks/1`).flush({}, { status: 403, statusText: 'Forbidden' });
    expect(store.endSession).not.toHaveBeenCalled();
  });
});
