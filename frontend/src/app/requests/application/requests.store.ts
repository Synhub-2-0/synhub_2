import { IamStore } from '../../iam/application/iam.store';
import { ToastStore } from '../../shared/application/toast.store';
import { Injectable, signal, inject, effect } from '@angular/core';
import { Observable } from 'rxjs';
import { RequestStatus, RequestType, TaskRequest } from '../domain/model/task-request.entity';
import { CreateRequestCommand } from '../domain/model/create-request.command';
import { RequestsApi } from '../infrastructure/requests.api';

// The backend requires a description, but the member does not write one: a fixed text is sent.
const SUBMISSION_DESCRIPTION = 'Tarea completada.';

@Injectable({ providedIn: 'root' })
export class RequestsStore {
  private requestsApi = inject(RequestsApi);

  private readonly toastStore = inject(ToastStore);
  private readonly iamStore = inject(IamStore);
  private readonly groupRequestsSignal = signal<TaskRequest[]>([]);
  private readonly requestsByTaskSignal = signal<Record<number, TaskRequest[]>>({});
  private readonly statusFilterSignal = signal<RequestStatus>('PENDING');
  private readonly typeFilterSignal = signal<RequestType>('SUBMISSION');
  private readonly loadingSignal = signal<boolean>(false);
  private readonly processingSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly groupRequests = this.groupRequestsSignal.asReadonly();
  readonly requestsByTask = this.requestsByTaskSignal.asReadonly();
  readonly statusFilter = this.statusFilterSignal.asReadonly();
  readonly typeFilter = this.typeFilterSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly processing = this.processingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  constructor() {
    // Root stores outlive the session: drop the previous user's data as soon as the session ends.
    effect(() => {
      if (!this.iamStore.isSignedIn()) this.reset();
    });
  }

  reset(): void {
    this.groupRequestsSignal.set([]);
    this.requestsByTaskSignal.set({});
    this.statusFilterSignal.set('PENDING');
    this.typeFilterSignal.set('SUBMISSION');
    this.errorSignal.set(null);
  }

  /** Leader inbox: the requests of the group of the chosen type (submissions, modifications or expirations) and status. */
  loadGroupRequests(
    groupId: number,
    status: RequestStatus = this.statusFilterSignal(),
    type: RequestType = this.typeFilterSignal(),
  ): void {
    this.statusFilterSignal.set(status);
    this.typeFilterSignal.set(type);
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.requestsApi.getRequestsByGroup(groupId, status, type).subscribe({
      next: (requests) => {
        this.groupRequestsSignal.set(requests);
        this.loadingSignal.set(false);
      },
      error: (err) => {
        this.groupRequestsSignal.set([]);
        this.setError(err.message);
        this.loadingSignal.set(false);
      },
    });
  }

  approveRequest(request: TaskRequest): void {
    this.review(this.requestsApi.approveRequest(request.id));
  }

  rejectRequest(request: TaskRequest): void {
    this.review(this.requestsApi.rejectRequest(request.id));
  }

  /**
   * The member completes an IN_PROGRESS task by sending it to validation (no comment). The backend marks the task
   * COMPLETED, so the created request carries the updated task.
   */
  submitTask(taskId: number, onSuccess?: (created: TaskRequest) => void): void {
    this.create(taskId, SUBMISSION_DESCRIPTION, 'SUBMISSION', onSuccess);
  }

  /**
   * The member asks the leader to change an IN_PROGRESS task. The backend puts the task ON_HOLD (its clock stops) until
   * the leader edits it, so the created request carries the updated task.
   */
  createModification(taskId: number, description: string, onSuccess?: (created: TaskRequest) => void): void {
    this.create(taskId, description.trim(), 'MODIFICATION', onSuccess);
  }

  loadTaskRequests(taskId: number): void {
    this.requestsApi.getRequestsByTask(taskId).subscribe({
      next: (requests) =>
        this.requestsByTaskSignal.update((cache) => ({ ...cache, [taskId]: requests })),
      error: () => this.requestsByTaskSignal.update((cache) => ({ ...cache, [taskId]: [] })),
    });
  }

  isUnderReview(taskId: number): boolean {
    return this.hasPending(taskId, 'SUBMISSION');
  }

  /** A pending modification keeps the task ON_HOLD until the leader edits it. */
  hasPendingModification(taskId: number): boolean {
    return this.hasPending(taskId, 'MODIFICATION');
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  private hasPending(taskId: number, type: RequestType): boolean {
    return (this.requestsByTask()[taskId] ?? []).some(
      (request) => request.requestType === type && request.requestStatus === 'PENDING',
    );
  }

  private create(
    taskId: number,
    description: string,
    requestType: RequestType,
    onSuccess?: (created: TaskRequest) => void,
  ): void {
    this.processingSignal.set(true);
    this.errorSignal.set(null);
    this.requestsApi.createRequest(new CreateRequestCommand({ taskId, description, requestType })).subscribe({
      next: (created) => {
        this.requestsByTaskSignal.update((cache) => ({
          ...cache,
          [taskId]: [...(cache[taskId] ?? []), created],
        }));
        this.processingSignal.set(false);
        onSuccess?.(created);
      },
      error: (err) => {
        this.setError(err.message);
        this.processingSignal.set(false);
      },
    });
  }

  private review(call$: Observable<TaskRequest>): void {
    this.processingSignal.set(true);
    this.errorSignal.set(null);
    call$.subscribe({
      next: (reviewed) => {
        // A reviewed request leaves the pending inbox; in the other tabs it just takes its new status.
        this.groupRequestsSignal.update((requests) =>
          this.statusFilterSignal() === 'PENDING'
            ? requests.filter((r) => r.id !== reviewed.id)
            : requests.map((r) => (r.id === reviewed.id ? reviewed : r)),
        );
        this.processingSignal.set(false);
      },
      error: (err) => {
        this.setError(err.message);
        this.processingSignal.set(false);
      },
    });
  }

  private setError(message: string): void {
    this.errorSignal.set(message);
    this.toastStore.error(message);
  }
}
