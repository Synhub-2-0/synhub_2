import { Component, Input, OnChanges } from '@angular/core';
import { AgCharts } from 'ag-charts-angular';
import { AgCartesianChartOptions } from 'ag-charts-community';
import { RescheduledTasks } from '../../../domain/model/rescheduled-tasks.model';

@Component({
  selector: 'app-rescheduled-chart',
  standalone: true,
  imports: [AgCharts],
  templateUrl: './rescheduled-chart.html',
})
export class RescheduledChart implements OnChanges {
  @Input() rescheduled: RescheduledTasks | null = null;
  @Input() loading = false;

  chartOptions: AgCartesianChartOptions = { series: [] };

  ngOnChanges(): void {
    this.buildOptions();
  }

  private buildOptions(): void {
    if (!this.rescheduled || this.rescheduled.total === 0) return;

    const { total, rescheduled } = this.rescheduled;
    const data = [
      { estado: 'En plazo', tareas: total - rescheduled },
      { estado: 'Reprogramadas', tareas: rescheduled },
    ];

    this.chartOptions = {
      data,
      background: { visible: false },
      padding: { top: 8, right: 8, bottom: 8, left: 8 },
      series: [
        {
          type: 'line',
          xKey: 'estado',
          yKey: 'tareas',
          yName: 'Tareas',
          stroke: '#4A90E2',
          strokeWidth: 3,
          marker: {
            enabled: true,
            size: 8,
            fill: '#4A90E2',
            stroke: '#fff',
            strokeWidth: 2,
          },
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
