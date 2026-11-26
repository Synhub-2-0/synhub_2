import { TaskDistributionChart } from './task-distribution-chart';

describe('TaskDistributionChart', () => {
  it('configura una gráfica donut con asignación por integrante y total en el centro', () => {
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

    expect(component.chartOptions.series).toEqual([
      expect.objectContaining({
        type: 'donut',
        angleKey: 'tasks',
        calloutLabelKey: 'member',
        innerLabels: expect.arrayContaining([expect.objectContaining({ text: '3' })]),
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
