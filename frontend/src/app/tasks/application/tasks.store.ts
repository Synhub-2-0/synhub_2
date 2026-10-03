import { IamStore } from '../../iam/application/iam.store';
import { ToastStore } from '../../shared/application/toast.store';
import { computed, Injectable, Signal, signal, inject, effect } from '@angular/core';
import { Task, TaskStatus } from '../domain/model/task.entity';
import { CreateTaskCommand } from '../domain/model/create-task.command';
import { UpdateTaskCommand } from '../domain/model/update-task.command';
import { TasksApi } from '../infrastructure/tasks.api';
import { catchError, forkJoin, map, of, retry } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TasksStore {
  private readonly toastStore = inject(ToastStore);
  private readonly iamStore = inject(IamStore);
  private readonly tasksSignal = signal<Task[]>([]);
  private readonly selectedTaskSignal = signal<Task | null>(null);
  private readonly statusFilterSignal = signal<TaskStatus | 'ALL'>('ALL');
  private readonly assignedGroupIdsSignal = signal<number[]>([]);
  private readonly loadingSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly tasks = this.tasksSignal.asReadonly();
  readonly selectedTask = this.selectedTaskSignal.asReadonly();
  readonly statusFilter = this.statusFilterSignal.asReadonly();
  readonly assignedGroupIds = this.assignedGroupIdsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly taskCount = computed(() => this.tasks().length);

  readonly filteredTasks = computed(() => {
    const filter = this.statusFilterSignal();
    const all = this.tasksSignal();
    return filter === 'ALL' ? all : all.filter((t) => t.status === filter);
  });

  constructor(private tasksApi: TasksApi) {
    // Root stores outlive the session: drop the previous user's data as soon as the session ends.
    effect(() => {
      if (!this.iamStore.isSignedIn()) this.reset();
    });
  }

  reset(): void {
    this.tasksSignal.set([]);
    this.selectedTaskSignal.set(null);
    this.statusFilterSignal.set('ALL');
    this.assignedGroupIdsSignal.set([]);
    this.errorSignal.set(null);
  }

  setStatusFilter(status: TaskStatus | 'ALL'): void {
    this.statusFilterSignal.set(status);
  }

  getTaskById(id: number): Signal<Task | undefined> {
    return computed(() => (id ? this.tasks().find((t) => t.id === id) : undefined));
  }

  /** Carga todas las tareas de un grupo (y opcionalmente filtradas por status en el backend) */
  loadTasksByGroup(groupId: number, status?: TaskStatus): void {
    this.tasksSignal.set([]);
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.tasksApi
      .getTasksByGroup(groupId, status)
      .pipe(retry(2))
      .subscribe({
        next: (tasks) => {
          this.tasksSignal.set(tasks);
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.setError(this.formatError(err, 'Failed to load group tasks'));
          this.loadingSignal.set(false);
        },
      });
  }

  /** Carga todas las tareas asignadas a un usuario específico dentro de un grupo */
  loadTasksByGroupAndUser(groupId: number, userId: number): void {
    this.tasksSignal.set([]);
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.tasksApi
      .getTasksByGroupAndUser(groupId, userId)
      .pipe(retry(2))
      .subscribe({
        next: (tasks) => {
          this.tasksSignal.set(tasks);
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.setError(this.formatError(err, 'Failed to load user tasks'));
          this.loadingSignal.set(false);
        },
      });
  }

  /** Detects, among the given groups, the ones where the user has at least one assigned task. */
  loadAssignedGroupIds(groupIds: number[], userId: number): void {
    if (groupIds.length === 0) {
      this.assignedGroupIdsSignal.set([]);
      return;
    }
    forkJoin(
      groupIds.map((groupId) =>
        this.tasksApi.getTasksByGroupAndUser(groupId, userId).pipe(
          map((tasks) => (tasks.length > 0 ? groupId : null)),
          catchError(() => of(null)),
        ),
      ),
    ).subscribe((ids) => this.assignedGroupIdsSignal.set(ids.filter((id): id is number => id !== null)));
  }

  /** Obtiene una tarea individual desde el backend y actualiza el estado local */
  loadTaskById(taskId: number): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.tasksApi
      .getTaskById(taskId)
      .pipe(retry(2))
      .subscribe({
        next: (task) => {
          this.selectedTaskSignal.set(task);
          this.tasksSignal.update((tasks) => {
            const exists = tasks.some((t) => t.id === task.id);
            return exists ? tasks.map((t) => (t.id === task.id ? task : t)) : [...tasks, task];
          });
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.setError(this.formatError(err, 'Failed to load task'));
          this.loadingSignal.set(false);
        },
      });
  }

  /** POST /api/v1/tasks */
  addTask(command: CreateTaskCommand): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.tasksApi
      .createTask(command)
      .pipe(retry(2))
      .subscribe({
        next: (createdTask) => {
          this.tasksSignal.update((tasks) => [...tasks, createdTask]);
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.setError(this.formatError(err, 'Failed to create task'));
          this.loadingSignal.set(false);
        },
      });
  }

  /** PUT /api/v1/tasks/{taskId} */
  updateTask(taskId: number, command: UpdateTaskCommand): void {
    if (this.isDone(taskId)) {
      this.setError(this.lockedMessage);
      return;
    }
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.tasksApi
      .updateTask(taskId, command)
      .pipe(retry(2))
      .subscribe({
        next: (updatedTask) => {
          this.tasksSignal.update((tasks) =>
            tasks.map((t) => (t.id === updatedTask.id ? updatedTask : t)),
          );
          if (this.selectedTaskSignal()?.id === updatedTask.id) {
            this.selectedTaskSignal.set(updatedTask);
          }
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.setError(this.formatError(err, 'Failed to update task'));
          this.loadingSignal.set(false);
        },
      });
  }

  /** PUT /api/v1/tasks/{taskId}/status?status=... */
  updateTaskStatus(taskId: number, status: TaskStatus): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.tasksApi
      .updateTaskStatus(taskId, status)
      .pipe(retry(2))
      .subscribe({
        next: (updatedTask) => {
          this.tasksSignal.update((tasks) =>
            tasks.map((t) => (t.id === updatedTask.id ? updatedTask : t)),
          );
          if (this.selectedTaskSignal()?.id === updatedTask.id) {
            this.selectedTaskSignal.set(updatedTask);
          }
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.setError(this.formatError(err, 'Failed to update task status'));
          this.loadingSignal.set(false);
        },
      });
  }

  /** DELETE /api/v1/tasks/{taskId} */
  deleteTask(id: number): void {
    if (this.isDone(id)) {
      this.setError(this.lockedMessage);
      return;
    }
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.tasksApi
      .deleteTask(id)
      .pipe(retry(2))
      .subscribe({
        next: () => {
          this.tasksSignal.update((tasks) => tasks.filter((t) => t.id !== id));
          if (this.selectedTaskSignal()?.id === id) {
            this.selectedTaskSignal.set(null);
          }
          this.loadingSignal.set(false);
          this.errorSignal.set(null);
        },
        error: (err) => {
          this.setError(this.formatError(err, 'Failed to delete task'));
          this.loadingSignal.set(false);
        },
      });
  }

  private formatError(error: any, fallback: string): string {
    if (error instanceof Error) {
      return error.message.includes('Resource not found')
        ? `${fallback}: Not found`
        : error.message;
    }
    return fallback;
  }

  private readonly lockedMessage = 'Una tarea terminada no puede editarse ni eliminarse.';

  private isDone(taskId: number): boolean {
    const task = this.tasksSignal().find((t) => t.id === taskId) ?? this.selectedTaskSignal();
    return task?.id === taskId && task.status === 'DONE';
  }

  private setError(message: string): void {
    this.errorSignal.set(message);
    this.toastStore.error(message);
  }
}
