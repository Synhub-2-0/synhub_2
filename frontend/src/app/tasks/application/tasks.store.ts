import {computed, Injectable, Signal, signal} from '@angular/core';
import {Task} from '../domain/model/task.entity';
import {TasksApi} from '../infrastructure/tasks.api';
import {retry} from 'rxjs';

@Injectable({providedIn: 'root'})
export class TasksStore {

  readonly taskCount = computed(() => this.tasks().length);

  private readonly tasksSignal = signal<Task[]>([])
  private readonly loadingSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly tasks = this.tasksSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  constructor(private tasksApi: TasksApi) {
    this.loadTasks();
  }

  getTaskById(id: number): Signal<Task | undefined> {
    return computed(() => (id ? this.tasks().find((t) => t.id === id) : undefined));
  }

  addTask(task: Task): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.tasksApi
      .createTask(task)
      .pipe(retry(2))
      .subscribe({
        next: (createdTask) => {
          this.tasksSignal.update((tasks) => [...tasks, createdTask]);
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.errorSignal.set(this.formatError(err, 'Failed to create task'));
          this.loadingSignal.set(false);
        }
      });
  }

  updateTask(updatedTask: Task): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.tasksApi
      .updateTask(updatedTask)
      .pipe(retry(2))
      .subscribe({
        next: (task) => {
          this.tasksSignal.update((tasks) =>
          tasks.map((t) => (t.id === task.id ? task : t)),
          );
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.errorSignal.set(this.formatError(err, 'Failed to update task'));
          this.loadingSignal.set(false);
        }
      });
  }

  deleteTask(id: number): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.tasksApi
      .deleteTask(id)
      .pipe(retry(2))
      .subscribe({
        next: () => {
          this.tasksSignal.update((tasks) => tasks.filter((t) => t.id !== id));
          this.loadingSignal.set(false);
          this.errorSignal.set(null);
        },
        error: (err) => {
          this.errorSignal.set(this.formatError(err, 'Failed to delete task'));
          this.loadingSignal.set(false);
        },
      });
  }

  // TODO: BACKEND TEAM YOU FORGOT THE CORE BUSINESS ENDPOINTS
  // I should not commit that
  private loadTasks(): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

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
