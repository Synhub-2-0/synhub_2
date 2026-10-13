/**
 * Pista local de la sesión (HUM-04).
 *
 * El JWT vive en una cookie HttpOnly que el navegador envía solo y que el JavaScript no puede leer.
 * En localStorage queda únicamente el id del usuario y el vencimiento de la sesión (1 día), para que
 * el guard y los stores sepan si hay una sesión vigente sin llamar a la API.
 */
const USER_ID_KEY = 'userId';
const EXPIRES_AT_KEY = 'sessionExpiresAt';

export function saveSession(userId: number, expiresAt: string): void {
  localStorage.setItem(USER_ID_KEY, String(userId));
  localStorage.setItem(EXPIRES_AT_KEY, expiresAt);
}

export function clearStoredSession(): void {
  localStorage.removeItem(USER_ID_KEY);
  localStorage.removeItem(EXPIRES_AT_KEY);
  // Versiones anteriores guardaban el JWT aquí.
  localStorage.removeItem('token');
}

export function storedUserId(): number | null {
  const value = localStorage.getItem(USER_ID_KEY);
  return value ? Number(value) : null;
}

/** Hay una sesión guardada y todavía no venció. */
export function hasActiveSession(now: number = Date.now()): boolean {
  const expiresAt = Date.parse(localStorage.getItem(EXPIRES_AT_KEY) ?? '');
  return storedUserId() !== null && !Number.isNaN(expiresAt) && expiresAt > now;
}
