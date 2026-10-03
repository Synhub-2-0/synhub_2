import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
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
    expect(store.report()).toBe('# Informe');
  });
});
