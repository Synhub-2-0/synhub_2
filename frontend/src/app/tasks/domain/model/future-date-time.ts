/**
 * ¿El valor de un `<input type="datetime-local">` es una fecha y hora futura? Se usa para reanudar una tarea vencida:
 * con una fecha pasada volvería a vencer en menos de un minuto.
 */
export function isFutureDateTime(value: string): boolean {
  if (!value) return false;
  const time = new Date(value).getTime();
  return !Number.isNaN(time) && time > Date.now();
}
