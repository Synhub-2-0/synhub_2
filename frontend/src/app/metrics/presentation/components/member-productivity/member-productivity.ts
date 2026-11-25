import { DecimalPipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MemberMetrics } from '../../../application/metrics.store';

export interface MemberOption {
  id: number;
  name: string;
}

/** Productividad individual de un integrante (US-012): el líder elige a cualquiera, el integrante solo se ve a sí mismo. */
@Component({
  selector: 'app-member-productivity',
  standalone: true,
  imports: [DecimalPipe, FormsModule],
  templateUrl: './member-productivity.html',
})
export class MemberProductivity {
  @Input() options: MemberOption[] = [];
  @Input() selectedId: number | null = null;
  @Input() metrics: MemberMetrics | null = null;
  @Input() loading = false;
  @Input() error: string | null = null;
  @Output() memberSelected = new EventEmitter<number>();

  get expired(): number {
    return this.metrics?.overview.details['EXPIRED'] ?? 0;
  }

  get inProgress(): number {
    const details = this.metrics?.overview.details ?? {};
    return (details['IN_PROGRESS'] ?? 0) + (details['ON_HOLD'] ?? 0) + (details['COMPLETED'] ?? 0);
  }

  get done(): number {
    return this.metrics?.overview.details['DONE'] ?? 0;
  }

  select(id: number): void {
    this.memberSelected.emit(Number(id));
  }
}
