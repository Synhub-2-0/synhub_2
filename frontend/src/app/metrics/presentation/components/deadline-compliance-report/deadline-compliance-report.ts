import { DecimalPipe } from '@angular/common';
import { Component, Input } from '@angular/core';
import { AvgCompletionTime } from '../../../domain/model/avg-completion-time.model';
import { TaskOverview } from '../../../domain/model/task-overview.model';

/** Reporte de cumplimiento de plazos del grupo (US-021). */
@Component({
  selector: 'app-deadline-compliance-report',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './deadline-compliance-report.html',
})
export class DeadlineComplianceReport {
  @Input() avgCompletion: AvgCompletionTime | null = null;
  @Input() overview: TaskOverview | null = null;
  @Input() loading = false;
  @Input() error: string | null = null;

  get total(): number {
    return this.overview?.total ?? 0;
  }

  get expired(): number {
    return this.overview?.details['EXPIRED'] ?? 0;
  }

  get expiredPercentage(): number {
    return this.total > 0 ? (this.expired / this.total) * 100 : 0;
  }
}
