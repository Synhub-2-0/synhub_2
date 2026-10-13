import { routes } from './app.routes';
import { iamGuard } from './iam/infrastructure/iam.guard';
import { PageNotFound } from './shared/presentation/views/page-not-found/page-not-found';

describe('Rutas de la aplicación', () => {
  const byPath = (path: string) => routes.find((r) => r.path === path);

  it('todas las rutas privadas usan iamGuard', () => {
    for (const path of ['home', 'groups', 'tasks', 'validations', 'recorrido']) {
      expect(byPath(path)?.canActivate, path).toContain(iamGuard);
    }
  });

  it('las rutas de autenticación son públicas', () => {
    expect(byPath('auth')?.canActivate).toBeUndefined();
  });

  it('la raíz redirige al login y lo desconocido va a la 404', () => {
    expect(byPath('')).toMatchObject({ redirectTo: '/auth/sign-in', pathMatch: 'full' });
    expect(byPath('**')?.component).toBe(PageNotFound);
    expect(routes[routes.length - 1].path).toBe('**');
  });

  it('las features se cargan de forma diferida', () => {
    for (const path of ['auth', 'groups', 'tasks', 'validations']) {
      expect(typeof byPath(path)?.loadChildren, path).toBe('function');
    }
    expect(typeof byPath('recorrido')?.loadComponent).toBe('function');
  });
});
