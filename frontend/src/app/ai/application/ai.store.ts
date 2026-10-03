import { Injectable, signal } from '@angular/core';
import { TaskClassification } from '../domain/model/task-classification.entity';
import { AiApi } from '../infrastructure/ai.api';

@Injectable({ providedIn: 'root' })
export class AiStore {
  private readonly classificationSignal = signal<TaskClassification | null>(null);
  private readonly classifyingSignal = signal<boolean>(false);
  private readonly reportSignal = signal<string | null>(null);
  private readonly reportLoadingSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly classification = this.classificationSignal.asReadonly();
  readonly classifying = this.classifyingSignal.asReadonly();
  readonly report = this.reportSignal.asReadonly();
  readonly reportLoading = this.reportLoadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  constructor(private readonly aiApi: AiApi) {}

  classifyTask(payload: { title: string; description: string; dueDate?: Date }): void {
    this.classifyingSignal.set(true);
    this.errorSignal.set(null);
    this.aiApi.classifyTask(payload).subscribe({
      next: (classification) => {
        this.classificationSignal.set(classification);
        this.classifyingSignal.set(false);
      },
      error: () => {
        this.errorSignal.set('La IA no está disponible ahora. Puedes completar los campos a mano.');
        this.classifyingSignal.set(false);
      },
    });
  }

  clearClassification(): void {
    this.classificationSignal.set(null);
    this.errorSignal.set(null);
  }

  loadReport(groupId: number): void {
    this.reportLoadingSignal.set(true);
    this.errorSignal.set(null);
    this.aiApi.getGroupReport(groupId).subscribe({
      next: (report) => {
        this.reportSignal.set(report);
        this.reportLoadingSignal.set(false);
      },
      error: () => {
        this.errorSignal.set('No se pudo generar el informe ahora. Intenta de nuevo.');
        this.reportLoadingSignal.set(false);
      },
    });
  }
}
