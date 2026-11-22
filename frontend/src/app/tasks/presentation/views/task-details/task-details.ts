import { CommonModule, Location } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { IamStore } from '../../../../iam/application/iam.store';
import { TasksStore } from '../../../application/tasks.store';
import { TaskMemberActions } from '../../components/task-member-actions/task-member-actions';
import { Task, TaskStatus } from '../../../domain/model/task.entity';
import { CONTEXT_LABELS, DIFFICULTY_LABELS, DIFFICULTY_SCALE, URGENCY_LABELS } from '../../../../ai/domain/model/task-classification.entity';

@Component({
  selector: 'app-task-details',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule, TaskMemberActions],
  templateUrl: './task-details.html',
  styleUrl: './task-details.css',
})
export class TaskDetails implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  private readonly router = inject(Router);
  private readonly iamStore = inject(IamStore);
  readonly tasksStore = inject(TasksStore);
  readonly difficultyScale = DIFFICULTY_SCALE;
  readonly contextLabels = CONTEXT_LABELS;
  readonly urgencyLabels = URGENCY_LABELS;
  readonly difficultyLabels = DIFFICULTY_LABELS;

  labelsOf(labels?: string): string[] {
    return (labels ?? '').split(',').map((l) => l.trim()).filter(Boolean);
  }

  private readonly statusLabels: Record<TaskStatus, string> = {
    ON_HOLD: 'En espera',
    IN_PROGRESS: 'En progreso',
    COMPLETED: 'Completada',
    DONE: 'Terminada',
    EXPIRED: 'Vencida',
  };

  labelFor(status: TaskStatus): string {
    return this.statusLabels[status] ?? status;
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) this.tasksStore.loadTaskById(id);
  }

  /** The member assigned to the task can complete it or ask for a modification from here too. */
  isAssignee(task: Task): boolean {
    const userId = this.iamStore.currentUserId();
    return userId !== null && task.assignedTo?.id === userId;
  }

  openGroup(task: Task): void {
    if (task.group) this.router.navigate(['/groups', task.group.id]).then();
  }

  goBack(): void { this.location.back(); }
}
