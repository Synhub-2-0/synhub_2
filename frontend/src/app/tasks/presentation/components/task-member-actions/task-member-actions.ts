import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { Task } from '../../../domain/model/task.entity';
import { TasksStore } from '../../../application/tasks.store';
import { RequestsStore } from '../../../../requests/application/requests.store';

/**
 * What the assigned member can do with a task: complete it (sends it to validation) or ask the leader to modify it.
 * Shared by the task card and the task detail so both offer the same options.
 */
@Component({
  selector: 'app-task-member-actions',
  standalone: true,
  imports: [MatIconModule],
  templateUrl: './task-member-actions.html',
  styleUrl: './task-member-actions.css',
})
export class TaskMemberActions implements OnInit {
  readonly requestsStore = inject(RequestsStore);
  private readonly tasksStore = inject(TasksStore);

  @Input({ required: true }) task!: Task;

  readonly showModificationForm = signal(false);
  readonly modificationComment = signal('');

  ngOnInit(): void {
    // A COMPLETED task may already have a submission waiting for the leader; an ON_HOLD one, a pending modification.
    if (this.task.status === 'COMPLETED' || this.task.status === 'ON_HOLD') {
      this.requestsStore.loadTaskRequests(this.task.id);
    }
  }

  toggleModificationForm(event: Event): void {
    event.stopPropagation();
    this.requestsStore.clearError();
    this.showModificationForm.update((open) => !open);
  }

  onModificationInput(event: Event): void {
    this.modificationComment.set((event.target as HTMLTextAreaElement).value);
  }

  /** Asking for a modification puts the task ON_HOLD (its time stops) until the leader edits it. */
  onRequestModification(event: Event): void {
    event.stopPropagation();
    const comment = this.modificationComment().trim();
    if (!comment) return;
    this.requestsStore.createModification(this.task.id, comment, (created) => {
      this.tasksStore.applyUpdatedTask(created.task);
      this.showModificationForm.set(false);
      this.modificationComment.set('');
    });
  }

  // Completing an IN_PROGRESS task sends it to validation; the backend marks it COMPLETED and returns it updated.
  onComplete(event: Event): void {
    event.stopPropagation();
    this.requestsStore.clearError();
    this.requestsStore.submitTask(this.task.id, (created) => this.tasksStore.applyUpdatedTask(created.task));
  }
}
