import { TaskOverview } from '../../../domain/model/task-overview.model';
import { TaskStatusOverview } from './task-status-overview';

describe('TaskStatusOverview', () => {
  const overview: TaskOverview = {
    type: 'TASK_OVERVIEW',
    total: 6,
    details: {
      DONE: 1,
      EXPIRED: 1,
      IN_PROGRESS: 2,
      ON_HOLD: 1,
      COMPLETED: 1,
    },
  };

  it('agrupa los estados para el donut y calcula el porcentaje sobre el total', () => {
    const component = new TaskStatusOverview();
    component.overview = overview;

    component.ngOnChanges();

    expect(component.total).toBe(6);
    expect(component.states.map(({ key, label, count, color }) => ({ key, label, count, color }))).toEqual([
      { key: 'DONE', label: 'Terminadas', count: 1, color: '#a855f7' },
      { key: 'EXPIRED', label: 'Vencidas / En riesgo', count: 1, color: '#f43f5e' },
      { key: 'IN_PROGRESS', label: 'En progreso', count: 2, color: '#3b82f6' },
      { key: 'WAITING', label: 'En espera', count: 2, color: '#f59e0b' },
    ]);
    expect(component.states[0].percentage).toBeCloseTo(100 / 6);
    expect(component.states[1].percentage).toBeCloseTo(100 / 6);
    expect(component.states[2].percentage).toBeCloseTo(200 / 6);
    expect(component.states[3].percentage).toBeCloseTo(200 / 6);
    expect(component.chartOptions.series).toEqual([
      expect.objectContaining({ type: 'donut', angleKey: 'count', innerRadiusRatio: 0.7 }),
    ]);
  });

  it('muestra cero en todos los estados y mantiene vacío el donut para un grupo sin tareas', () => {
    const component = new TaskStatusOverview();
    component.overview = { type: 'TASK_OVERVIEW', total: 0, details: {} };

    component.ngOnChanges();

    expect(component.states.every((state) => state.count === 0 && state.percentage === 0)).toBe(true);
    expect(component.chartOptions.series).toEqual([]);
  });

  it('limpia los datos cuando se reinicia la entrada', () => {
    const component = new TaskStatusOverview();
    component.overview = overview;
    component.ngOnChanges();
    component.overview = null;
    component.ngOnChanges();

    expect(component.states).toEqual([]);
    expect(component.total).toBe(0);
    expect(component.chartOptions.series).toEqual([]);
  });
});
