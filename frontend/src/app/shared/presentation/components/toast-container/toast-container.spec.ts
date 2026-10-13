import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastContainer } from './toast-container';
import { ToastStore } from '../../../application/toast.store';

describe('ToastContainer', () => {
  let fixture: ComponentFixture<ToastContainer>;
  let store: ToastStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ToastContainer] }).compileComponents();
    fixture = TestBed.createComponent(ToastContainer);
    store = TestBed.inject(ToastStore);
  });

  const alerts = () => Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('[role="alert"]'));

  it('muestra los toasts del store con su título', async () => {
    store.success('Grupo creado');
    store.error('Solo el líder puede editar.');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(alerts()).toHaveLength(2);
    expect(alerts()[0].textContent).toContain('Listo');
    expect(alerts()[0].textContent).toContain('Grupo creado');
    expect(alerts()[1].textContent).toContain('Error');
  });

  it('el botón cerrar quita el toast', async () => {
    store.info('Hola');
    fixture.detectChanges();
    (alerts()[0].querySelector('button') as HTMLButtonElement).click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(store.toasts()).toHaveLength(0);
    expect(alerts()).toHaveLength(0);
  });
});
