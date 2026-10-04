import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { AiApi } from '../infrastructure/ai.api';
import { AiStore } from './ai.store';
import { TaskClassification } from '../domain/model/task-classification.entity';

describe('AiStore', () => {
  const suggestion: TaskClassification = {
    labels: ['Login'], urgency: 'HIGH', difficulty: 3, context: 'FRONTEND', rationale: 'Bug crítico',
  };
  let api: { classifyTask: ReturnType<typeof vi.fn>; getGroupReport: ReturnType<typeof vi.fn> };
  let store: AiStore;

  beforeEach(() => {
    api = { classifyTask: vi.fn(), getGroupReport: vi.fn() };
    TestBed.configureTestingModule({ providers: [{ provide: AiApi, useValue: api }] });
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
