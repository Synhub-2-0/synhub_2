import { Component, computed, inject, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { AiStore } from '../../../application/ai.store';
import { parseReport } from '../../../domain/model/task-classification.entity';

@Component({
  selector: 'app-ai-report-card',
  standalone: true,
  imports: [MatIconModule, DatePipe],
  templateUrl: './ai-report-card.html',
})
export class AiReportCard {
  readonly aiStore = inject(AiStore);
  readonly groupId = input<number | null>(null);
  readonly report = computed(() => this.aiStore.reportFor(this.groupId()));
  readonly loading = computed(() => this.aiStore.isReportLoading(this.groupId()));
  readonly error = computed(() => this.aiStore.reportErrorFor(this.groupId()));
  readonly blocks = computed(() => parseReport(this.report()?.text ?? ''));

  generate(): void {
    const groupId = this.groupId();
    if (groupId) this.aiStore.loadReport(groupId);
  }
}
