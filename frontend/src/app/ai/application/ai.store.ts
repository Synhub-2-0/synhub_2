import { Injectable, signal, inject, effect } from '@angular/core';
import { IamStore } from '../../iam/application/iam.store';
import { GroupReport, TaskClassification } from '../domain/model/task-classification.entity';
import { AiApi } from '../infrastructure/ai.api';

@Injectable({ providedIn: 'root' })
export class AiStore {
  private readonly aiApi = inject(AiApi);
  private readonly iamStore = inject(IamStore);
  // Cada sugerencia pertenece al formulario que la pidió: una respuesta de una petición anterior se descarta.
  private classifyRequestId = 0;
  // Cambia al cerrar sesión: un informe pedido en la sesión anterior no se guarda en la siguiente.
  private sessionId = 0;

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

  constructor() {
    // Root stores outlive the session: drop the previous user's data as soon as the session ends.
    effect(() => {
      if (!this.iamStore.isSignedIn()) this.reset();
    });
  }

  reset(): void {
    this.sessionId++;
    this.clearClassification();
    this.reportsSignal.set({});
    this.reportLoadingGroupSignal.set(null);
    this.reportErrorSignal.set(null);
  }

  reportFor(groupId: number | null): GroupReport | null {
    return groupId == null ? null : (this.reportsSignal()[groupId] ?? null);
  }

  isReportLoading(groupId: number | null): boolean {
    return groupId != null && this.reportLoadingGroupSignal() === groupId;
  }

  reportErrorFor(groupId: number | null): string | null {
    const error = this.reportErrorSignal();
    return error && error.groupId === groupId ? error.message : null;
  }

  classifyTask(payload: { title: string; description: string; dueDate?: Date }): void {
    const requestId = ++this.classifyRequestId;
    this.classifyingSignal.set(true);
    this.errorSignal.set(null);
    this.aiApi.classifyTask(payload).subscribe({
      next: (classification) => {
        if (requestId !== this.classifyRequestId) return;
        this.classificationSignal.set(classification);
        this.classifyingSignal.set(false);
      },
      error: () => {
        if (requestId !== this.classifyRequestId) return;
        this.errorSignal.set('La IA no está disponible ahora. Puedes completar los campos a mano.');
        this.classifyingSignal.set(false);
      },
    });
  }

  /** Descarta la sugerencia actual y cualquier petición en curso (al abrir o cerrar un formulario). */
  clearClassification(): void {
    this.classifyRequestId++;
    this.classificationSignal.set(null);
    this.classifyingSignal.set(false);
    this.errorSignal.set(null);
  }

  loadReport(groupId: number): void {
    const sessionId = this.sessionId;
    this.reportLoadingGroupSignal.set(groupId);
    this.reportErrorSignal.set(null);
    this.aiApi.getGroupReport(groupId).subscribe({
      next: (text) => {
        if (sessionId !== this.sessionId) return;
        // Se guarda bajo el grupo pedido aunque el usuario ya haya cambiado de grupo.
        this.reportsSignal.update((all) => ({
          ...all,
          [groupId]: { text, generatedAt: new Date() },
        }));
        if (this.reportLoadingGroupSignal() === groupId) this.reportLoadingGroupSignal.set(null);
      },
      error: () => {
        if (sessionId !== this.sessionId) return;
        this.reportErrorSignal.set({
          groupId,
          message: 'No se pudo generar el informe ahora. Intenta de nuevo.',
        });
        if (this.reportLoadingGroupSignal() === groupId) this.reportLoadingGroupSignal.set(null);
      },
    });
  }
}
