import {
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  viewChild,
} from '@angular/core';
import { RescheduledTasks } from '../../../domain/model/rescheduled-tasks.model';
import * as CanvasJS from '@canvasjs/charts';

@Component({
  selector: 'app-rescheduled-chart',
  standalone: true,
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
      <div #chartContainer class="h-48 w-full"></div>
    }
  `,
})
export class RescheduledChart implements OnChanges, OnDestroy {
  @Input() rescheduled: RescheduledTasks | null = null;
  @Input() loading = false;

  private readonly chartContainer = viewChild<ElementRef<HTMLDivElement>>('chartContainer');
  private chart: CanvasJS.Chart | null = null;

  ngOnChanges(): void {
    Promise.resolve().then(() => this.renderChart());
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
    this.chart = null;
  }

  private renderChart(): void {
    const container = this.chartContainer()?.nativeElement;
    if (!container || !this.rescheduled || this.rescheduled.total === 0) return;

    const { total, rescheduled } = this.rescheduled;
    const onTime = total - rescheduled;

    const dataPoints = [
      { x: new Date(2000, 0, 1), y: onTime },
      { x: new Date(2000, 0, 2), y: rescheduled },
    ];

    if (this.chart) {
      this.chart.options.data![0].dataPoints = dataPoints;
      this.chart.render();
      return;
    }

    this.chart = new CanvasJS.Chart(container, {
      animationEnabled: true,
      backgroundColor: 'transparent',
      title: { text: '' },
      axisX: {
        labelFormatter: (e: { value: number | Date }) => {
          const d = e.value instanceof Date ? e.value : new Date(e.value);
          return d.getDate() === 1 ? 'En plazo' : 'Reprogramadas';
        },
        labelFontSize: 12,
        labelFontColor: '#64748b',
        tickLength: 0,
        lineThickness: 0,
        interval: 1,
        intervalType: 'day',
      },
      axisY: {
        gridThickness: 1,
        gridColor: '#f1f5f9',
        labelFontSize: 11,
        labelFontColor: '#94a3b8',
        lineThickness: 0,
        tickLength: 0,
        minimum: 0,
      },
      toolTip: {
        borderThickness: 0,
        cornerRadius: 8,
        content: '{label}: <strong>{y} tarea(s)</strong>',
      },
      data: [
        {
          type: 'line',
          lineThickness: 3,
          markerType: 'circle',
          markerSize: 10,
          color: '#4A90E2',
          dataPoints,
        },
      ],
    });

    this.chart.render();
  }
}
