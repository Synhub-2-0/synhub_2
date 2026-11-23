import { Component, Input, OnChanges } from '@angular/core';
import { AgCharts } from 'ag-charts-angular';
import { AgCartesianChartOptions } from 'ag-charts-community';
import { TaskDistribution } from '../../../domain/model/task-distribution.model';

@Component({
  selector: 'app-task-distribution-chart',
  standalone: true,
  imports: [AgCharts],
  template: `
    @if (loading) {
      <div class="flex h-56 items-center justify-center text-sm text-slate-400">Cargando...</div>
    } @else if (!distribution || distribution.total === 0) {
      <div class="flex h-56 items-center justify-center text-sm text-slate-400">Sin datos de distribución.</div>
    } @else {
      <ag-charts [options]="chartOptions" class="block h-56 w-full"></ag-charts>
    }
  `,
})
export class TaskDistributionChart implements OnChanges {
  @Input() distribution: TaskDistribution | null = null;
  @Input() loading = false;

  chartOptions: AgCartesianChartOptions = { series: [] };

  ngOnChanges(): void {
    this.buildOptions();
  }

  private buildOptions(): void {
    if (!this.distribution || this.distribution.total === 0) return;

    const data = Object.values(this.distribution.details).map((info) => ({
      member: info.memberName,
      tasks: info.taskCount,
    }));

    this.chartOptions = {
      data,
      background: { visible: false },
      padding: { top: 8, right: 8, bottom: 8, left: 8 },
      series: [
        {
          type: 'bar',
          xKey: 'member',
          yKey: 'tasks',
          yName: 'Tareas',
          fill: '#4A90E2',
          strokeWidth: 0,
          cornerRadius: 4,
        },
      ],
      axes: {
        x: {
          type: 'category',
          position: 'bottom',
          line: { enabled: false },
          tick: { enabled: false },
          label: { fontSize: 12, color: '#64748b' },
        },
        y: {
          type: 'number',
          position: 'left',
          line: { enabled: false },
          tick: { enabled: false },
          label: { fontSize: 11, color: '#94a3b8' },
        },
      },
      legend: { enabled: false },
    };
  }
}
