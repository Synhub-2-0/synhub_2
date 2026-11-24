import { Component, Input, OnChanges } from '@angular/core';
import { AgCharts } from 'ag-charts-angular';
import { AgCartesianChartOptions } from 'ag-charts-community';
import { TaskDistribution } from '../../../domain/model/task-distribution.model';

@Component({
  selector: 'app-task-distribution-chart',
  standalone: true,
  imports: [AgCharts],
  templateUrl: './task-distribution-chart.html',
})
export class TaskDistributionChart implements OnChanges {
  @Input() distribution: TaskDistribution | null = null;
  @Input() loading = false;

  chartOptions: AgCartesianChartOptions = { series: [] };
  hasData = false;

  ngOnChanges(): void {
    this.buildOptions();
  }

  private buildOptions(): void {
    const members = Object.values(this.distribution?.details ?? {}).filter(
      (member) => member.taskCount > 0,
    );
    this.hasData = members.length > 0;
    if (!this.distribution || members.length === 0) {
      this.chartOptions = { series: [] };
      return;
    }

    const data = members.map((info) => ({
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
          yName: 'Tareas asignadas',
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
