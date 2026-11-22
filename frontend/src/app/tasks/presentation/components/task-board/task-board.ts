import { CommonModule } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { Task, TaskStatus } from '../../../domain/model/task.entity';
import { TaskResolveDialog } from '../task-resolve-dialog/task-resolve-dialog';

interface KanbanColumn {
  status: TaskStatus;
  title: string;
  dotClass: string;
}

@Component({
  selector: 'app-task-board',
  standalone: true,
  imports: [CommonModule, TaskResolveDialog],
  templateUrl: './task-board.html',
  styleUrl: './task-board.css',
})
export class TaskBoard {
  readonly tasks = input<Task[]>([]);
  // Only the leader of the group moves tasks between statuses; members just see the board.
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
    // A locked group (its leader exceeded the plan limit) is read-only.
    if (task.group?.locked) return false;
    return this.canManageAll();
  }

  statusLabel(status: TaskStatus): string {
    return this.columns.find((column) => column.status === status)?.title ?? status;
  }

  initialsOf(task: Task): string {
    const user = task.assignedTo;
    return `${user?.name?.[0] ?? ''}${user?.surname?.[0] ?? ''}`.toUpperCase() || 'U';
  }

  /** Task whose status change is waiting for the leader to solve its problem first (dialog open). */
  readonly resolving = signal<{ task: Task; status: TaskStatus } | null>(null);

  /**
   * Changing the status of an EXPIRED task (needs a new due date) or an ON_HOLD one (may wait for a modification
   * request) opens a dialog to solve the problem; the other changes go straight through.
   */
  onStatusChange(task: Task, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const status = select.value as TaskStatus;
    if (task.status === 'EXPIRED' || task.status === 'ON_HOLD') {
      select.value = task.status; // the task did not change yet: the selector goes back to its status
      this.resolving.set({ task, status });
      return;
    }
    this.statusChange.emit({ taskId: task.id, status });
  }

  /** The ON_HOLD task had nothing to solve: the status change the leader asked for goes on. */
  proceedResolving(): void {
    const pending = this.resolving();
    this.resolving.set(null);
    if (pending) this.statusChange.emit({ taskId: pending.task.id, status: pending.status });
  }

  finishResolving(): void {
    this.resolving.set(null);
  }
}
