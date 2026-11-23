import { Component, Input, OnChanges } from '@angular/core';
import { AgCharts } from 'ag-charts-angular';
import { AgCartesianChartOptions } from 'ag-charts-community';
import { RescheduledTasks } from '../../../domain/model/rescheduled-tasks.model';

@Component({
  selector: 'app-rescheduled-chart',
  standalone: true,
  imports: [AgCharts],
  template: `
    @if (loading) {
      <div class="flex h-56 items-center justify-center text-sm text-slate-400">Cargando...</div>
    } @else if (!rescheduled || rescheduled.total === 0) {
      <div class="flex h-56 items-center justify-center text-sm text-slate-400">Sin tareas registradas aún.</div>
    } @else {
      <div class="mb-3 flex items-center gap-6 text-sm">
        <span class="text-slate-500">Total de tareas: <strong class="text-slate-800">{{ rescheduled.total }}</strong></span>
        <span class="text-slate-500">Reprogramadas: <strong class="text-[#E24A4A]">{{ rescheduled.rescheduled }}</strong></span>
      </div>
      <ag-charts [options]="chartOptions" class="block h-48 w-full"></ag-charts>
    }
  `,
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
