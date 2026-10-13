import { TestBed } from '@angular/core/testing';
import { ToastStore } from './toast.store';

describe('ToastStore', () => {
  let store: ToastStore;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({});
    store = TestBed.inject(ToastStore);
  });

  afterEach(() => vi.useRealTimers());

  it('agrega toasts de cada tipo y los cierra', () => {
    store.success('Guardado');
    store.info('Hola');
    store.error('Mensaje en español');
    expect(store.toasts().map((t) => t.type)).toEqual(['success', 'info', 'error']);
    store.dismiss(store.toasts()[0].id);
    expect(store.toasts().map((t) => t.message)).toEqual(['Hola', 'Mensaje en español']);
  });

  it('no repite el mismo mensaje mientras sigue visible', () => {
    store.success('Guardado');
    store.success('Guardado');
    expect(store.toasts()).toHaveLength(1);
  });

  it('desaparecen solos: 4 s los informativos y 6 s los errores', () => {
    store.success('Guardado');
    store.error('Error visible');
    vi.advanceTimersByTime(4000);
    expect(store.toasts().map((t) => t.type)).toEqual(['error']);
    vi.advanceTimersByTime(2000);
    expect(store.toasts()).toHaveLength(0);
  });

  it.each([
    ['Failed to load groups: 401', 'Tu sesión expiró. Vuelve a iniciar sesión.'],
    ['Failed to load groups: 403', 'No tienes permisos para realizar esta acción.'],
    ['Failed to load task: Resource not found', 'No se encontró lo que buscas.'],
    ['Failed to update task: 409', 'La acción entra en conflicto con el estado actual. Recarga e inténtalo de nuevo.'],
    ['Failed to delete task: 500', 'El servidor tuvo un problema. Inténtalo más tarde.'],
    ['Failed to fetch entities: Unexpected error', 'No se pudo conectar con el servidor.'],
    ['Failed to sign-in: 401', 'Usuario o contraseña incorrectos.'],
    ['Failed to sign-in: 500', 'No se pudo iniciar sesión. Inténtalo nuevamente.'],
    ['Failed to sign-up: 409', 'No se pudo crear la cuenta: revisa los datos, el usuario o el correo ya podrían estar en uso.'],
    ['', 'Ocurrió un error inesperado.'],
  ])('traduce "%s" a un mensaje amable', (technical, friendly) => {
    store.error(technical);
    expect(store.toasts()[0].message).toBe(friendly);
  });

  it('los mensajes que ya son para el usuario no se tocan', () => {
    store.error('Solo el líder del grupo puede enviar invitaciones.');
    expect(store.toasts()[0].message).toBe('Solo el líder del grupo puede enviar invitaciones.');
  });
});
