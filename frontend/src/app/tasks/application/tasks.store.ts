import { computed, Injectable, Signal, signal } from '@angular/core';
import { Task, TaskStatus } from '../domain/model/task.entity';
import { CreateTaskCommand } from '../domain/model/create-task.command';
import { UpdateTaskCommand } from '../domain/model/update-task.command';
import { TasksApi } from '../infrastructure/tasks.api';
import { retry } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TasksStore {
  private readonly tasksSignal = signal<Task[]>([]);
  private readonly selectedTaskSignal = signal<Task | null>(null);
  private readonly statusFilterSignal = signal<TaskStatus | 'ALL'>('ALL');
  private readonly loadingSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly tasks = this.tasksSignal.asReadonly();
  readonly selectedTask = this.selectedTaskSignal.asReadonly();
  readonly statusFilter = this.statusFilterSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly taskCount = computed(() => this.tasks().length);

  readonly filteredTasks = computed(() => {
    const filter = this.statusFilterSignal();
    const all = this.tasksSignal();
    return filter === 'ALL' ? all : all.filter((t) => t.status === filter);
  });

  constructor(private tasksApi: TasksApi) {}

  setStatusFilter(status: TaskStatus | 'ALL'): void {
    this.statusFilterSignal.set(status);
  }

  getTaskById(id: number): Signal<Task | undefined> {
    return computed(() => (id ? this.tasks().find((t) => t.id === id) : undefined));
  }

  /** Carga todas las tareas de un grupo (y opcionalmente filtradas por status en el backend) */
  loadTasksByGroup(groupId: number, status?: TaskStatus): void {
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
          this.errorSignal.set(this.formatError(err, 'Failed to load group tasks'));
          this.loadingSignal.set(false);
        },
      });
  }

  /** Carga todas las tareas asignadas a un usuario específico dentro de un grupo */
  loadTasksByGroupAndUser(groupId: number, userId: number): void {
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
          this.errorSignal.set(this.formatError(err, 'Failed to load user tasks'));
          this.loadingSignal.set(false);
        },
      });
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
          this.errorSignal.set(this.formatError(err, 'Failed to load task'));
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
          this.errorSignal.set(this.formatError(err, 'Failed to create task'));
          this.loadingSignal.set(false);
        },
      });
  }

  /** PUT /api/v1/tasks/{taskId} */
  updateTask(taskId: number, command: UpdateTaskCommand): void {
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
          this.errorSignal.set(this.formatError(err, 'Failed to update task'));
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
          this.errorSignal.set(this.formatError(err, 'Failed to update task status'));
          this.loadingSignal.set(false);
        },
      });
  }

  /** DELETE /api/v1/tasks/{taskId} */
  deleteTask(id: number): void {
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
          this.errorSignal.set(this.formatError(err, 'Failed to delete task'));
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
}
