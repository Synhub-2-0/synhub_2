import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { Task, TaskStatus } from '../../../domain/model/task.entity';
import { TasksStore } from '../../../application/tasks.store';
import { RequestsStore } from '../../../../requests/application/requests.store';
import { CONTEXT_LABELS, DIFFICULTY_LABELS, DIFFICULTY_SCALE, URGENCY_LABELS } from '../../../../ai/domain/model/task-classification.entity';

@Component({
  selector: 'app-task-card',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  templateUrl: './task-card.html',
  styleUrl: './task-card.css',
})
export class TaskCard implements OnInit {
  private readonly router = inject(Router);

  readonly requestsStore = inject(RequestsStore);
  private readonly tasksStore = inject(TasksStore);

  @Input({ required: true }) task!: Task;
  @Input() isLeader = false;
  @Input() showGroup = false;
  @Output() deleteRequested = new EventEmitter<number>();

  readonly difficultyScale = DIFFICULTY_SCALE;
  readonly contextLabels = CONTEXT_LABELS;
  readonly urgencyLabels = URGENCY_LABELS;
  readonly difficultyLabels = DIFFICULTY_LABELS;

  ngOnInit(): void {
    // A COMPLETED task of the member may already have a submission waiting for the leader.
    if (!this.isLeader && this.task.status === 'COMPLETED') {
      this.requestsStore.loadTaskRequests(this.task.id);
    }
  }

  get initials(): string {
    const name = this.task.assignedTo?.name ?? '';
    const surname = this.task.assignedTo?.surname ?? '';
    return `${name[0] ?? ''}${surname[0] ?? ''}`.toUpperCase() || 'T';
  }

  // A task approved by the leader is final: it cannot be edited or deleted.
  get isLocked(): boolean {
    return this.task.status === 'DONE';
  }

  get statusBadgeClass(): string {
    const classes: Record<TaskStatus, string> = {
      COMPLETED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
      DONE: 'bg-purple-50 text-purple-700 border-purple-200',
      IN_PROGRESS: 'bg-[#e0efff] text-[#1A4E85] border-[#b8daff]',
      ON_HOLD: 'bg-amber-50 text-amber-700 border-amber-200',
      EXPIRED: 'bg-red-50 text-red-700 border-red-200',
    };
    return classes[this.task.status] ?? 'bg-slate-100 text-slate-700 border-slate-200';
  }

  get statusLabel(): string {
    const labels: Record<TaskStatus, string> = {
      ON_HOLD: 'En espera',
      IN_PROGRESS: 'En progreso',
      COMPLETED: 'Completada',
      DONE: 'Terminada',
      EXPIRED: 'Vencida',
    };
    return labels[this.task.status] ?? this.task.status;
  }

  get progressClass(): string {
    const classes: Record<TaskStatus, string> = {
      ON_HOLD: 'bg-amber-500',
      DONE: 'bg-purple-500',
      COMPLETED: 'bg-emerald-500',
      EXPIRED: 'bg-red-500',
      IN_PROGRESS: 'bg-[#4A90E2]',
    };
    return classes[this.task.status] ?? 'bg-[#4A90E2]';
  }

  get remainingTimeLabel(): string {
    if (!this.task.dueDate) return '—';
    const diff = new Date(this.task.dueDate).getTime() - Date.now();
    if (diff <= 0) return 'Plazo vencido';
    const days = Math.floor(diff / 86_400_000);
    const hours = Math.floor((diff % 86_400_000) / 3_600_000);
    return `${days}d ${hours}h restantes`;
  }

  openDetails(): void {
    this.router.navigate(['/tasks', this.task.id]).then();
  }

  onEdit(event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/tasks', this.task.id, 'edit']).then();
  }

  onDelete(event: Event): void {
    event.stopPropagation();
    this.deleteRequested.emit(this.task.id);
  }

  onReviewSubmission(event: Event): void {
    event.stopPropagation();
    this.router
      .navigate(['/validations'], { queryParams: { groupId: this.task.group?.id } })
      .then();
  }

  // Completing an IN_PROGRESS task sends it to validation; the backend marks it COMPLETED and returns it updated.
  onComplete(event: Event): void {
    event.stopPropagation();
    this.requestsStore.clearError();
    this.requestsStore.submitTask(this.task.id, (created) => this.tasksStore.applyUpdatedTask(created.task));
  }
}
