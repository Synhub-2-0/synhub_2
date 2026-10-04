import { Injectable, signal } from '@angular/core';
import { GroupReport, TaskClassification } from '../domain/model/task-classification.entity';
import { AiApi } from '../infrastructure/ai.api';

@Injectable({ providedIn: 'root' })
export class AiStore {
  private readonly classificationSignal = signal<TaskClassification | null>(null);
  private readonly classifyingSignal = signal<boolean>(false);
  // Un informe por grupo: cambiar de grupo nunca muestra el informe de otro.
  private readonly reportsSignal = signal<Record<number, GroupReport>>({});
  private readonly reportLoadingGroupSignal = signal<number | null>(null);
  private readonly reportErrorSignal = signal<{ groupId: number; message: string } | null>(null);
  private readonly errorSignal = signal<string | null>(null);

  readonly classification = this.classificationSignal.asReadonly();
  readonly classifying = this.classifyingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  reportFor(groupId: number | null): GroupReport | null {
    return groupId == null ? null : this.reportsSignal()[groupId] ?? null;
  }

  isReportLoading(groupId: number | null): boolean {
    return groupId != null && this.reportLoadingGroupSignal() === groupId;
  }

  reportErrorFor(groupId: number | null): string | null {
    const error = this.reportErrorSignal();
    return error && error.groupId === groupId ? error.message : null;
  }

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
    this.reportLoadingGroupSignal.set(groupId);
    this.reportErrorSignal.set(null);
    this.aiApi.getGroupReport(groupId).subscribe({
      next: (text) => {
        // Se guarda bajo el grupo pedido aunque el usuario ya haya cambiado de grupo.
        this.reportsSignal.update((all) => ({ ...all, [groupId]: { text, generatedAt: new Date() } }));
        if (this.reportLoadingGroupSignal() === groupId) this.reportLoadingGroupSignal.set(null);
      },
      error: () => {
        this.reportErrorSignal.set({ groupId, message: 'No se pudo generar el informe ahora. Intenta de nuevo.' });
        if (this.reportLoadingGroupSignal() === groupId) this.reportLoadingGroupSignal.set(null);
      },
    });
  }
}
