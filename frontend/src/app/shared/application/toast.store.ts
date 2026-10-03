import { Injectable, signal } from '@angular/core';

export type ToastType = 'error' | 'success' | 'info';

export interface Toast {
  id: number;
  type: ToastType;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ToastStore {
  private readonly toastsSignal = signal<Toast[]>([]);
  private nextId = 1;

  readonly toasts = this.toastsSignal.asReadonly();

  error(message: string): void {
    this.show('error', this.toFriendlyMessage(message), 6000);
  }

  success(message: string): void {
    this.show('success', message, 4000);
  }

  info(message: string): void {
    this.show('info', message, 4000);
  }

  dismiss(id: number): void {
    this.toastsSignal.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  private show(type: ToastType, message: string, durationMs: number): void {
    // The same message is not stacked twice while it is still visible.
    if (this.toastsSignal().some((toast) => toast.type === type && toast.message === message)) return;
    const id = this.nextId++;
    this.toastsSignal.update((toasts) => [...toasts, { id, type, message }]);
    setTimeout(() => this.dismiss(id), durationMs);
  }

  /**
   * Store and endpoint errors look like "Failed to load groups: 403" or "...: Resource not found".
   * Messages that are already user-facing (Spanish) are shown untouched.
   */
  private toFriendlyMessage(message: string): string {
    if (!message) return 'Ocurrió un error inesperado.';
    if (!/failed to|resource not found|not found|unexpected/i.test(message)) return message;

    const status = /:\s*(\d{3})\s*$/.exec(message)?.[1];
    const code = status ? Number(status) : /not found/i.test(message) ? 404 : 0;

    if (/sign-in/i.test(message)) {
      return [401, 403, 404].includes(code)
        ? 'Usuario o contraseña incorrectos.'
        : 'No se pudo iniciar sesión. Inténtalo nuevamente.';
    }
    if (/sign-up/i.test(message)) {
      return code === 409 || code === 400
        ? 'No se pudo crear la cuenta: revisa los datos, el usuario o el correo ya podrían estar en uso.'
        : 'No se pudo crear la cuenta. Inténtalo nuevamente.';
    }

    switch (true) {
      case code === 401: return 'Tu sesión expiró. Vuelve a iniciar sesión.';
      case code === 403: return 'No tienes permisos para realizar esta acción.';
      case code === 404: return 'No se encontró lo que buscas.';
      case code === 409: return 'La acción entra en conflicto con el estado actual. Recarga e inténtalo de nuevo.';
      case code >= 500: return 'El servidor tuvo un problema. Inténtalo más tarde.';
      case code === 0: return 'No se pudo conectar con el servidor.';
      default: return 'No se pudo completar la acción. Inténtalo nuevamente.';
    }
  }
}
