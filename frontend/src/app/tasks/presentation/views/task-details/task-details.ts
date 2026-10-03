import { CommonModule, Location } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TasksStore } from '../../../application/tasks.store';
import { TaskStatus } from '../../../domain/model/task.entity';
import { DIFFICULTY_SCALE } from '../../../../ai/domain/model/task-classification.entity';

@Component({
  selector: 'app-task-details',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  templateUrl: './task-details.html',
  styleUrl: './task-details.css',
})
export class TaskDetails implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  readonly tasksStore = inject(TasksStore);
  readonly difficultyScale = DIFFICULTY_SCALE;

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

  goBack(): void { this.location.back(); }
}
