import { Component, computed, inject, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { AiStore } from '../../../application/ai.store';
import { parseReport } from '../../../domain/model/task-classification.entity';

@Component({
  selector: 'app-ai-report-card',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './ai-report-card.html',
})
export class AiReportCard {
  readonly aiStore = inject(AiStore);
  readonly groupId = input<number | null>(null);
  readonly blocks = computed(() => parseReport(this.aiStore.report() ?? ''));

  generate(): void {
    const groupId = this.groupId();
    if (groupId) this.aiStore.loadReport(groupId);
  }
}
