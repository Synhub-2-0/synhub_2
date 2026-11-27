import { TaskDistributionChart } from './task-distribution-chart';

describe('TaskDistributionChart', () => {
  it('configura una gráfica de barras con las tareas asignadas por integrante', () => {
    const component = new TaskDistributionChart();
    component.distribution = {
      type: 'TASK_DISTRIBUTION',
      total: 3,
      details: {
        '1': { memberName: 'Ana', taskCount: 2 },
        '2': { memberName: 'Luis', taskCount: 1 },
      },
    };

    component.ngOnChanges();

    expect(component.hasData).toBe(true);
    expect(component.chartOptions.data).toEqual([
      { member: 'Ana', tasks: 2 },
      { member: 'Luis', tasks: 1 },
    ]);
    expect(component.chartOptions.series).toEqual([
      expect.objectContaining({
        type: 'bar',
        xKey: 'member',
        yKey: 'tasks',
        yName: 'Tareas asignadas',
      }),
    ]);
  });

  it('limpia la gráfica cuando no hay tareas asignadas', () => {
    const component = new TaskDistributionChart();
    component.distribution = { type: 'TASK_DISTRIBUTION', total: 0, details: {} };

    component.ngOnChanges();

    expect(component.chartOptions.series).toEqual([]);
  });
});
