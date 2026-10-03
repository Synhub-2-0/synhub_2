import { CommonModule } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { Task, TaskStatus } from '../../../domain/model/task.entity';

interface KanbanColumn {
  status: TaskStatus;
  title: string;
  dotClass: string;
}

@Component({
  selector: 'app-task-board',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './task-board.html',
  styleUrl: './task-board.css',
})
export class TaskBoard {
  readonly tasks = input<Task[]>([]);
  // Members can only move their own tasks; the leader of the group can move any of them.
  readonly currentUserId = input<number | null>(null);
  readonly canManageAll = input(false);
  readonly statusChange = output<{ taskId: number; status: TaskStatus }>();

  readonly columns: KanbanColumn[] = [
    { status: 'IN_PROGRESS', title: 'En progreso', dotClass: 'bg-blue-500' },
    { status: 'ON_HOLD', title: 'En espera', dotClass: 'bg-amber-500' },
    { status: 'COMPLETED', title: 'Completadas', dotClass: 'bg-emerald-500' },
    { status: 'DONE', title: 'Terminadas', dotClass: 'bg-purple-500' },
    { status: 'EXPIRED', title: 'Vencidas', dotClass: 'bg-rose-500' },
  ];

  // DONE is not listed: it is only reached when the leader approves a submission (Validaciones).
  readonly selectableStatuses: { value: TaskStatus; label: string }[] = [
    { value: 'IN_PROGRESS', label: 'En progreso' },
    { value: 'ON_HOLD', label: 'En espera' },
    { value: 'COMPLETED', label: 'Completada' },
    { value: 'EXPIRED', label: 'Vencida' },
  ];

  readonly tasksByStatus = computed(() => {
    const taskMap: Record<TaskStatus, Task[]> = {
      IN_PROGRESS: [],
      ON_HOLD: [],
      COMPLETED: [],
      DONE: [],
      EXPIRED: [],
    };
    for (const task of this.tasks()) {
      taskMap[task.status]?.push(task);
    }
    return taskMap;
  });

  canChangeStatus(task: Task): boolean {
    if (task.status === 'DONE') return false;
    return this.canManageAll() || (this.currentUserId() !== null && task.assignedTo?.id === this.currentUserId());
  }

  statusLabel(status: TaskStatus): string {
    return this.columns.find((column) => column.status === status)?.title ?? status;
  }

  initialsOf(task: Task): string {
    const user = task.assignedTo;
    return `${user?.name?.[0] ?? ''}${user?.surname?.[0] ?? ''}`.toUpperCase() || 'U';
  }

  onStatusChange(taskId: number, event: Event): void {
    this.statusChange.emit({ taskId, status: (event.target as HTMLSelectElement).value as TaskStatus });
  }
}
