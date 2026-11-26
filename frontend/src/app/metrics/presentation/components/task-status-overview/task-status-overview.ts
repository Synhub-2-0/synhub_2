import { DecimalPipe } from '@angular/common';
import { Component, Input, OnChanges } from '@angular/core';
import { AgCharts } from 'ag-charts-angular';
import { AgPolarChartOptions } from 'ag-charts-community';
import { TaskOverview } from '../../../domain/model/task-overview.model';

interface StatusSlice {
  key: string;
  label: string;
  count: number;
  percentage: number;
  color: string;
}

@Component({
  selector: 'app-task-status-overview',
  standalone: true,
  imports: [AgCharts, DecimalPipe],
  templateUrl: './task-status-overview.html',
})
export class TaskStatusOverview implements OnChanges {
  @Input() overview: TaskOverview | null = null;
  @Input() loading = false;
  @Input() error: string | null = null;

  chartOptions: AgPolarChartOptions = { series: [] };
  states: StatusSlice[] = [];
  total = 0;

  ngOnChanges(): void {
    this.buildOverview();
  }

  private buildOverview(): void {
    if (!this.overview) {
      this.total = 0;
      this.states = [];
      this.chartOptions = { series: [] };
      return;
    }

    const details = this.overview.details;
    this.total = this.overview.total;
    this.states = [
      this.slice('DONE', 'Terminadas', details['DONE'] ?? 0, '#a855f7'),
      this.slice('EXPIRED', 'Vencidas / En riesgo', details['EXPIRED'] ?? 0, '#f43f5e'),
      this.slice('IN_PROGRESS', 'En progreso', details['IN_PROGRESS'] ?? 0, '#3b82f6'),
      this.slice('WAITING', 'En espera', (details['ON_HOLD'] ?? 0) + (details['COMPLETED'] ?? 0), '#f59e0b'),
    ];

    const chartData = this.states
      .filter((state) => state.count > 0)
      .map((state) => ({ status: state.label, count: state.count, color: state.color }));

    this.chartOptions = {
      data: chartData,
      background: { visible: false },
      padding: { top: 8, right: 8, bottom: 8, left: 8 },
      series: chartData.length
        ? [
            {
              type: 'donut',
              angleKey: 'count',
              angleName: 'Tareas',
              calloutLabelKey: 'status',
              innerRadiusRatio: 0.7,
              fills: chartData.map((item) => item.color),
              innerLabels: [
                { text: `${this.total}`, fontSize: 22, fontWeight: 'bold', color: '#1e293b' },
                { text: 'TAREAS', fontSize: 9, color: '#94a3b8' },
              ],
            },
          ]
        : [],
      legend: { enabled: false },
    };
  }

  private slice(key: string, label: string, count: number, color: string): StatusSlice {
    return {
      key,
      label,
      count,
      percentage: this.total > 0 ? (count / this.total) * 100 : 0,
      color,
    };
  }
}
