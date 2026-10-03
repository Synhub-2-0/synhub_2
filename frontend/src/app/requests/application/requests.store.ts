import {Injectable, signal} from '@angular/core';
import {Observable} from 'rxjs';
import {RequestStatus, TaskRequest} from '../domain/model/task-request.entity';
import {CreateRequestCommand} from '../domain/model/create-request.command';
import {RequestsApi} from '../infrastructure/requests.api';

@Injectable({providedIn: 'root'})
export class RequestsStore {
  private readonly submissionsSignal = signal<TaskRequest[]>([]);
  private readonly requestsByTaskSignal = signal<Record<number, TaskRequest[]>>({});
  private readonly statusFilterSignal = signal<RequestStatus>('PENDING');
  private readonly loadingSignal = signal<boolean>(false);
  private readonly processingSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly submissions = this.submissionsSignal.asReadonly();
  readonly requestsByTask = this.requestsByTaskSignal.asReadonly();
  readonly statusFilter = this.statusFilterSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly processing = this.processingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  constructor(private requestsApi: RequestsApi) {}

  /** Leader inbox: the SUBMISSION requests of every task of the group, filtered by status. */
  loadGroupSubmissions(groupId: number, status: RequestStatus = this.statusFilterSignal()): void {
    this.statusFilterSignal.set(status);
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.requestsApi
      .getRequestsByGroup(groupId, status, 'SUBMISSION')
      .subscribe({
        next: (requests) => {
          this.submissionsSignal.set(requests);
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.submissionsSignal.set([]);
          this.errorSignal.set(err.message);
          this.loadingSignal.set(false);
        }
      });
  }

  approveSubmission(request: TaskRequest): void {
    this.review(this.requestsApi.approveRequest(request.task.id, request.id));
  }

  rejectSubmission(request: TaskRequest): void {
    this.review(this.requestsApi.rejectRequest(request.task.id, request.id));
  }

  /** The task must already be COMPLETED: the member marks it before sending the submission. */
  submitTask(taskId: number, description: string, onSuccess?: () => void): void {
    this.processingSignal.set(true);
    this.errorSignal.set(null);
    this.requestsApi
      .createRequest(new CreateRequestCommand({taskId, description, requestType: 'SUBMISSION'}))
      .subscribe({
        next: (created) => {
          this.requestsByTaskSignal.update((cache) => ({...cache, [taskId]: [...(cache[taskId] ?? []), created]}));
          this.processingSignal.set(false);
          onSuccess?.();
        },
        error: (err) => {
          this.errorSignal.set(err.message);
          this.processingSignal.set(false);
        }
      });
  }

  loadTaskRequests(taskId: number): void {
    this.requestsApi.getRequestsByTask(taskId).subscribe({
      next: (requests) => this.requestsByTaskSignal.update((cache) => ({...cache, [taskId]: requests})),
      error: () => this.requestsByTaskSignal.update((cache) => ({...cache, [taskId]: []})),
    });
  }

  isUnderReview(taskId: number): boolean {
    return (this.requestsByTask()[taskId] ?? [])
      .some((request) => request.requestType === 'SUBMISSION' && request.requestStatus === 'PENDING');
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  private review(call$: Observable<TaskRequest>): void {
    this.processingSignal.set(true);
    this.errorSignal.set(null);
    call$.subscribe({
      next: (reviewed) => {
        // A reviewed request leaves the pending inbox; in the other tabs it just takes its new status.
        this.submissionsSignal.update((requests) => this.statusFilterSignal() === 'PENDING'
          ? requests.filter((r) => r.id !== reviewed.id)
          : requests.map((r) => (r.id === reviewed.id ? reviewed : r)));
        this.processingSignal.set(false);
      },
      error: (err) => {
        this.errorSignal.set(err.message);
        this.processingSignal.set(false);
      }
    });
  }
}
