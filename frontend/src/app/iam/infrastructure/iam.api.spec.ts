import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { IamApi } from './iam.api';
import { SignInCommand } from '../domain/model/sign-in.command';
import { environment } from '../../../environments/environment';

describe('IamApi (HTTP)', () => {
  const base = environment.platformProviderApiBaseUrl;
  let api: IamApi;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(IamApi);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('sign-in hace POST con usuario y contraseña', () => {
    let expiresAt = '';
    api.signIn(new SignInCommand({ username: 'ana', password: 'x' })).subscribe((r) => (expiresAt = r.expiresAt));
    const req = backend.expectOne(`${base}/authentication/sign-in`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'ana', password: 'x' });
    req.flush({ id: 5, username: 'ana', role: [], expiresAt: '2099-01-01T00:00:00Z' });
    expect(expiresAt).toBe('2099-01-01T00:00:00Z');
  });

  it('un error de sign-in se traduce a un mensaje con el código', () => {
    let message = '';
    api.signIn(new SignInCommand({ username: 'ana', password: 'x' })).subscribe({ error: (e: Error) => (message = e.message) });
    backend.expectOne(`${base}/authentication/sign-in`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(message).toBe('Failed to sign-in: 401');
  });

  it('usuario actual, por id y por username (codificado)', () => {
    api.autoSignIn().subscribe();
    api.getProfileByUserId(5).subscribe();
    api.getProfileByUsername('ana maría').subscribe();
    expect(backend.expectOne(`${base}/users/me`).request.method).toBe('GET');
    backend.expectOne(`${base}/users/5`);
    backend.expectOne(`${base}/users/username?username=ana%20mar%C3%ADa`);
  });

  it('un usuario inexistente produce "Resource not found"', () => {
    let message = '';
    api.getProfileByUsername('nadie').subscribe({ error: (e: Error) => (message = e.message) });
    backend.expectOne(`${base}/users/username?username=nadie`).flush({}, { status: 404, statusText: 'Not Found' });
    expect(message).toContain('Resource not found');
  });
});
