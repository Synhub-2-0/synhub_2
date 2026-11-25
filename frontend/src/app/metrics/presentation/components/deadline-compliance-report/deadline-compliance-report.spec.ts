import { DeadlineComplianceReport } from './deadline-compliance-report';

describe('DeadlineComplianceReport', () => {
  it('calcula el porcentaje de tareas vencidas o en riesgo sobre el total del grupo', () => {
    const component = new DeadlineComplianceReport();
    component.overview = { type: 'TASK_OVERVIEW', total: 8, details: { DONE: 5, EXPIRED: 2, IN_PROGRESS: 1 } };
    component.avgCompletion = { type: 'AVG_COMPLETION_TIME', days: 3.5, completedTasks: 5 };

    expect(component.total).toBe(8);
    expect(component.expired).toBe(2);
    expect(component.expiredPercentage).toBeCloseTo(25);
  });

  it('muestra cero vencidas cuando el grupo no tiene tareas', () => {
    const component = new DeadlineComplianceReport();
    component.overview = { type: 'TASK_OVERVIEW', total: 0, details: {} };

    expect(component.expired).toBe(0);
    expect(component.expiredPercentage).toBe(0);
  });
});
