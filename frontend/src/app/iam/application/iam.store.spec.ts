import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { IamApi } from '../infrastructure/iam.api';
import { IamStore } from './iam.store';
import { SignInCommand } from '../domain/model/sign-in.command';
import { SignUpCommand } from '../domain/model/sign-up.command';
import { Profile } from '../domain/model/profile.entity';
import { ToastStore } from '../../shared/application/toast.store';
import { saveSession } from '../infrastructure/session';

describe('IamStore', () => {
  const profile = new Profile({ id: 5, name: 'Ana', surname: 'Torres', email: 'ana@synhub.io', imgUrl: '', userId: 5, username: 'ana' });
  const future = '2099-01-01T00:00:00Z';
  let api: Record<'signIn' | 'signUp' | 'signOut' | 'autoSignIn' | 'getProfileByUserId', ReturnType<typeof vi.fn>>;
  let router: { navigate: ReturnType<typeof vi.fn> };
  let store: IamStore;
  let toasts: ToastStore;

  const createStore = () => {
    TestBed.configureTestingModule({ providers: [{ provide: IamApi, useValue: api }] });
    toasts = TestBed.inject(ToastStore);
    return TestBed.inject(IamStore);
  };

  beforeEach(() => {
    localStorage.clear();
    api = { signIn: vi.fn(), signUp: vi.fn(), signOut: vi.fn(), autoSignIn: vi.fn(), getProfileByUserId: vi.fn() };
    router = { navigate: vi.fn().mockResolvedValue(true) };
  });

  describe('signIn', () => {
    beforeEach(() => (store = createStore()));

    it('guarda userId y vencimiento (nunca el token), carga el perfil y navega a /home', () => {
      api.signIn.mockReturnValue(of({ id: 5, username: 'ana', role: ['ROLE_USER'], expiresAt: future }));
      api.getProfileByUserId.mockReturnValue(of(profile));

      store.signIn(new SignInCommand({ username: 'ana', password: 'x' }), router as unknown as Router);

      expect(localStorage.getItem('token')).toBeNull();
      expect(localStorage.getItem('userId')).toBe('5');
      expect(localStorage.getItem('sessionExpiresAt')).toBe(future);
      expect(store.isSignedIn()).toBe(true);
      expect(store.currentUserId()).toBe(5);
      expect(store.currentUsername()).toBe('ana');
      expect(store.currentProfile()).toBe(profile);
      expect(api.getProfileByUserId).toHaveBeenCalledWith(5);
      expect(router.navigate).toHaveBeenCalledWith(['/home']);
    });

    it('un perfil que no carga no impide la sesión', () => {
      api.signIn.mockReturnValue(of({ id: 5, username: 'ana', role: [], expiresAt: future }));
      api.getProfileByUserId.mockReturnValue(throwError(() => new Error('404')));
      store.signIn(new SignInCommand({ username: 'ana', password: 'x' }), router as unknown as Router);
      expect(store.isSignedIn()).toBe(true);
      expect(store.currentProfile()).toBeNull();
    });

    it('credenciales inválidas: sin sesión, toast amable y vuelve al login', () => {
      api.signIn.mockReturnValue(throwError(() => new Error('Failed to sign-in: 401')));

      store.signIn(new SignInCommand({ username: 'ana', password: 'mal' }), router as unknown as Router);

      expect(store.isSignedIn()).toBe(false);
      expect(localStorage.getItem('userId')).toBeNull();
      expect(toasts.toasts()[0].message).toBe('Usuario o contraseña incorrectos.');
      expect(router.navigate).toHaveBeenCalledWith(['/auth/sign-in']);
    });
  });

  describe('signUp', () => {
    const command = new SignUpCommand({
      username: 'ana', password: 'Secret123!', name: 'Ana', surname: 'Torres', email: 'ana@synhub.io', imgUrl: '',
    });

    beforeEach(() => (store = createStore()));

    it('tras registrarse inicia sesión con las mismas credenciales', () => {
      api.signUp.mockReturnValue(of({ id: 5 }));
      api.signIn.mockReturnValue(of({ id: 5, username: 'ana', role: [], expiresAt: future }));
      api.getProfileByUserId.mockReturnValue(of(profile));

      store.signUp(command, router as unknown as Router);

      const signInCommand = api.signIn.mock.calls[0][0] as SignInCommand;
      expect([signInCommand.username, signInCommand.password]).toEqual(['ana', 'Secret123!']);
      expect(store.isSignedIn()).toBe(true);
      expect(router.navigate).toHaveBeenCalledWith(['/home']);
    });

    it('si el registro falla vuelve al formulario con un toast', () => {
      api.signUp.mockReturnValue(throwError(() => new Error('Failed to sign-up: 409')));
      store.signUp(command, router as unknown as Router);
      expect(api.signIn).not.toHaveBeenCalled();
      expect(toasts.toasts()[0].message).toContain('No se pudo crear la cuenta');
      expect(router.navigate).toHaveBeenCalledWith(['/auth/sign-up']);
    });
  });

  it('signOut borra la cookie en el backend, limpia la sesión y navega al login', () => {
    saveSession(5, future);
    store = createStore();
    expect(store.isSignedIn()).toBe(true);
    api.signOut.mockReturnValue(of(undefined));

    store.signOut(router as unknown as Router);

    expect(api.signOut).toHaveBeenCalled();
    expect(store.isSignedIn()).toBe(false);
    expect(store.currentUserId()).toBeNull();
    expect(localStorage.getItem('userId')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/sign-in']);
  });

  it('signOut cierra la sesión local aunque el backend no responda', () => {
    saveSession(5, future);
    store = createStore();
    api.signOut.mockReturnValue(throwError(() => new Error('0')));
    store.signOut(router as unknown as Router);
    expect(store.isSignedIn()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/auth/sign-in']);
  });

  it('una sesión guardada pero vencida arranca cerrada', () => {
    saveSession(5, '2000-01-01T00:00:00Z');
    store = createStore();
    expect(store.isSignedIn()).toBe(false);
    expect(store.currentUserId()).toBeNull();
  });

  describe('restoreSession', () => {
    it('sin sesión no llama a la API', async () => {
      store = createStore();
      await store.restoreSession();
      expect(api.autoSignIn).not.toHaveBeenCalled();
      expect(store.isSignedIn()).toBe(false);
    });

    it('con sesión vigente recupera el usuario y su perfil', async () => {
      saveSession(5, future);
      store = createStore();
      api.autoSignIn.mockReturnValue(of({ id: 5, username: 'ana', accountRoles: [] }));
      api.getProfileByUserId.mockReturnValue(of(profile));

      await store.restoreSession();

      expect(store.currentUsername()).toBe('ana');
      expect(store.currentUserId()).toBe(5);
      expect(store.currentProfile()).toBe(profile);
      expect(localStorage.getItem('userId')).toBe('5');
    });

    it('si el backend rechaza la cookie limpia la sesión', async () => {
      saveSession(5, future);
      store = createStore();
      api.autoSignIn.mockReturnValue(throwError(() => new Error('401')));

      await store.restoreSession();

      expect(store.isSignedIn()).toBe(false);
      expect(localStorage.getItem('userId')).toBeNull();
    });
  });
});
