import {
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  viewChild,
} from '@angular/core';
import { TaskDistribution } from '../../../domain/model/task-distribution.model';
import * as CanvasJS from '@canvasjs/charts';

@Component({
  selector: 'app-task-distribution-chart',
  standalone: true,
  template: `
    @if (loading) {
      <div class="flex h-56 items-center justify-center text-sm text-slate-400">Cargando...</div>
    } @else if (!distribution || distribution.total === 0) {
      <div class="flex h-56 items-center justify-center text-sm text-slate-400">Sin datos de distribución.</div>
    } @else {
      <div #chartContainer class="h-56 w-full"></div>
    }
  `,
})
export class TaskDistributionChart implements OnChanges, OnDestroy {
  @Input() distribution: TaskDistribution | null = null;
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
    if (!container || !this.distribution || this.distribution.total === 0) return;

    const dataPoints = Object.values(this.distribution.details).map((info) => ({
      label: info.memberName,
      y: info.taskCount,
    }));

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
        labelFontSize: 12,
        labelFontColor: '#64748b',
        tickLength: 0,
        lineThickness: 0,
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
          type: 'column',
          color: '#4A90E2',
          cornerRadiusY: 6,
          dataPoints,
        },
      ],
    });

    this.chart.render();
  }
}
