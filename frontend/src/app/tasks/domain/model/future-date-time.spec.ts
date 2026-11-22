import { isFutureDateTime } from './future-date-time';

describe('isFutureDateTime', () => {
  const local = (offsetMs: number) => {
    const date = new Date(Date.now() + offsetMs);
    return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
  };

  it('una fecha y hora futura es válida', () => {
    expect(isFutureDateTime(local(2 * 86_400_000))).toBe(true);
  });

  it('una pasada no es válida', () => {
    expect(isFutureDateTime('2020-01-01T10:00')).toBe(false);
    expect(isFutureDateTime(local(-3_600_000))).toBe(false);
  });

  it('vacía o ilegible no es válida', () => {
    expect(isFutureDateTime('')).toBe(false);
    expect(isFutureDateTime('no es una fecha')).toBe(false);
  });
});
