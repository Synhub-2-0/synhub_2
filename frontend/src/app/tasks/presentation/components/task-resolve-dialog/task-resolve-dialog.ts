import { Component, EventEmitter, Input, OnInit, Output, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { Task } from '../../../domain/model/task.entity';
import { isFutureDateTime } from '../../../domain/model/future-date-time';
import { TasksStore } from '../../../application/tasks.store';
import { RequestsStore } from '../../../../requests/application/requests.store';

/**
 * Opens when the leader tries to change the status of a task that has a problem to solve first:
 * - EXPIRED: it needs a new (future) due date to resume.
 * - ON_HOLD because of a modification request from the member: edit the task (approves the change) or reject the request.
 * An ON_HOLD task with nothing pending has nothing to resolve: `proceed` lets the normal status change go on.
 */
@Component({
  selector: 'app-task-resolve-dialog',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './task-resolve-dialog.html',
  styleUrl: './task-resolve-dialog.css',
})
export class TaskResolveDialog implements OnInit {
  private readonly router = inject(Router);
  readonly tasksStore = inject(TasksStore);
  readonly requestsStore = inject(RequestsStore);

  @Input({ required: true }) task!: Task;
  @Output() closed = new EventEmitter<void>();
  @Output() proceed = new EventEmitter<void>();

  readonly newDueDate = signal('');
  /** An ON_HOLD task first looks for its pending modification request. */
  readonly checking = signal(false);

  readonly isExpired = computed(() => this.task?.status === 'EXPIRED');

  readonly pendingModification = computed(() =>
    (this.requestsStore.requestsByTask()[this.task.id] ?? []).find(
      (request) => request.requestType === 'MODIFICATION' && request.requestStatus === 'PENDING',
    ),
  );

  ngOnInit(): void {
    if (this.task.status !== 'ON_HOLD') return;
    this.checking.set(true);
    this.requestsStore.loadTaskRequests(this.task.id, () => {
      this.checking.set(false);
      if (!this.pendingModification()) this.proceed.emit();
    });
  }

  /** A new due date in the future, so the task does not expire again in a minute. */
  isDateValid(): boolean {
    return isFutureDateTime(this.newDueDate());
  }

  onDateInput(event: Event): void {
    this.newDueDate.set((event.target as HTMLInputElement).value);
  }

  resume(): void {
    if (!this.isDateValid()) return;
    this.tasksStore.resumeExpiredTask(this.task, new Date(this.newDueDate()), () => this.closed.emit());
  }

  /** Denying the modification puts the task back in IN_PROGRESS as it was. */
  deny(): void {
    const request = this.pendingModification();
    if (!request) return;
    this.requestsStore.rejectRequest(request, (reviewed) => {
      this.tasksStore.applyUpdatedTask(reviewed.task);
      this.closed.emit();
    });
  }

  /** Editing the task approves the change; after saving or cancelling the leader comes back to this screen. */
  edit(): void {
    this.router.navigate(['/tasks', this.task.id, 'edit'], { queryParams: { returnTo: this.router.url } }).then();
  }

  cancel(): void {
    this.closed.emit();
  }
}
