import { clearStoredSession, hasActiveSession, saveSession, storedUserId } from './session';

describe('session', () => {
  beforeEach(() => localStorage.clear());

  it('guarda el usuario y el vencimiento, nunca el token', () => {
    saveSession(5, '2099-01-01T00:00:00Z');
    expect(storedUserId()).toBe(5);
    expect(hasActiveSession()).toBe(true);
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('una sesión vencida, sin vencimiento o sin usuario no está activa', () => {
    saveSession(5, '2000-01-01T00:00:00Z');
    expect(hasActiveSession()).toBe(false);
    localStorage.setItem('sessionExpiresAt', 'no-es-fecha');
    expect(hasActiveSession()).toBe(false);
    localStorage.clear();
    localStorage.setItem('sessionExpiresAt', '2099-01-01T00:00:00Z');
    expect(hasActiveSession()).toBe(false);
  });

  it('clearStoredSession borra también el token de versiones anteriores', () => {
    localStorage.setItem('token', 'jwt-viejo');
    saveSession(5, '2099-01-01T00:00:00Z');
    clearStoredSession();
    expect(localStorage.length).toBe(0);
  });
});
