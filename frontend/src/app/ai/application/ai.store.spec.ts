import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Subject, of, throwError } from 'rxjs';
import { AiApi } from '../infrastructure/ai.api';
import { AiStore } from './ai.store';
import { IamStore } from '../../iam/application/iam.store';
import { TaskClassification } from '../domain/model/task-classification.entity';

describe('AiStore', () => {
  const suggestion: TaskClassification = {
    labels: ['Login'], urgency: 'HIGH', difficulty: 3, context: 'FRONTEND', rationale: 'Bug crítico',
  };
  let api: { classifyTask: ReturnType<typeof vi.fn>; getGroupReport: ReturnType<typeof vi.fn> };
  let isSignedIn: ReturnType<typeof signal<boolean>>;
  let store: AiStore;

  beforeEach(() => {
    api = { classifyTask: vi.fn(), getGroupReport: vi.fn() };
    isSignedIn = signal(true);
    TestBed.configureTestingModule({
      providers: [{ provide: AiApi, useValue: api }, { provide: IamStore, useValue: { isSignedIn } }],
    });
    store = TestBed.inject(AiStore);
  });

  it('guarda la clasificación sugerida', () => {
    api.classifyTask.mockReturnValue(of(suggestion));
    store.classifyTask({ title: 'Login falla', description: '' });
    expect(store.classification()).toEqual(suggestion);
    expect(store.classifying()).toBe(false);
  });

  it('expone un error amable si la IA falla y no deja clasificación', () => {
    api.classifyTask.mockReturnValue(throwError(() => new Error('503')));
    store.classifyTask({ title: 'Login falla', description: '' });
    expect(store.classification()).toBeNull();
    expect(store.error()).toContain('IA no está disponible');
  });

  it('una sugerencia tardía no aparece en el siguiente formulario', () => {
    const late = new Subject<TaskClassification>();
    api.classifyTask.mockReturnValue(late);
    store.classifyTask({ title: 'Tarea del grupo A', description: '' });
    store.clearClassification(); // se sale del formulario antes de que responda la IA
    expect(store.classifying()).toBe(false);
    late.next(suggestion);
    expect(store.classification()).toBeNull();
  });

  it('al volver a sugerir, solo cuenta la última respuesta', () => {
    const first = new Subject<TaskClassification>();
    api.classifyTask.mockReturnValueOnce(first).mockReturnValueOnce(of({ ...suggestion, difficulty: 5 }));
    store.classifyTask({ title: 'Login falla', description: '' });
    store.classifyTask({ title: 'Login falla', description: 'con más detalle' });
    first.next(suggestion);
    expect(store.classification()?.difficulty).toBe(5);
  });

  it('al cerrar sesión se vacían la sugerencia y los informes', () => {
    api.classifyTask.mockReturnValue(of(suggestion));
    api.getGroupReport.mockReturnValue(of('# Informe grupo 7'));
    store.classifyTask({ title: 'Login falla', description: '' });
    store.loadReport(7);
    isSignedIn.set(false);
    TestBed.tick();
    expect(store.classification()).toBeNull();
    expect(store.reportFor(7)).toBeNull();
  });

  it('un informe pedido en la sesión anterior no llega a la siguiente', () => {
    const late = new Subject<string>();
    api.getGroupReport.mockReturnValue(late);
    store.loadReport(7);
    isSignedIn.set(false);
    TestBed.tick();
    isSignedIn.set(true);
    TestBed.tick();
    late.next('# Informe de otra sesión');
    expect(store.reportFor(7)).toBeNull();
    expect(store.isReportLoading(7)).toBe(false);
  });

  it('carga el informe del grupo', () => {
    api.getGroupReport.mockReturnValue(of('# Informe'));
    store.loadReport(7);
    expect(api.getGroupReport).toHaveBeenCalledWith(7);
    expect(store.reportFor(7)?.text).toBe('# Informe');
  });

  it('no muestra el informe de un grupo en otro', () => {
    api.getGroupReport.mockReturnValue(of('# Informe grupo 7'));
    store.loadReport(7);
    expect(store.reportFor(8)).toBeNull();
    expect(store.isReportLoading(8)).toBe(false);
  });

  it('una respuesta tardía queda en su grupo y no en el actual', () => {
    const late = new Subject<string>();
    api.getGroupReport.mockReturnValueOnce(late).mockReturnValueOnce(of('# Informe grupo 8'));
    store.loadReport(7);
    store.loadReport(8);
    late.next('# Informe grupo 7');
    expect(store.reportFor(8)?.text).toBe('# Informe grupo 8');
    expect(store.reportFor(7)?.text).toBe('# Informe grupo 7');
  });

  it('el error del informe queda en su grupo y no afecta al formulario', () => {
    api.getGroupReport.mockReturnValue(throwError(() => new Error('503')));
    store.loadReport(7);
    expect(store.reportErrorFor(7)).toContain('No se pudo generar');
    expect(store.reportErrorFor(8)).toBeNull();
    expect(store.error()).toBeNull();
  });
});
