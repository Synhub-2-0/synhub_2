import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { TasksStore } from './tasks.store';
import { TasksApi } from '../infrastructure/tasks.api';
import { IamStore } from '../../iam/application/iam.store';
import { ToastStore } from '../../shared/application/toast.store';
import { Task, TaskStatus } from '../domain/model/task.entity';
import { CreateTaskCommand } from '../domain/model/create-task.command';
import { UpdateTaskCommand } from '../domain/model/update-task.command';

const task = (id: number, status: TaskStatus = 'IN_PROGRESS', due = '2026-12-01T10:00:00Z') => new Task({
  id, title: `T${id}`, description: '', dueDate: new Date(due), createdAt: new Date(), updatedAt: new Date(), status,
});

describe('TasksStore', () => {
  let api: Record<'getTasksByGroup' | 'getTasksByGroupAndUser' | 'getMyTasks' | 'getTaskById' | 'createTask' | 'updateTask' | 'updateTaskStatus' | 'deleteTask', ReturnType<typeof vi.fn>>;
  let isSignedIn: ReturnType<typeof signal<boolean>>;
  let store: TasksStore;
  let toasts: ToastStore;

  beforeEach(() => {
    api = {
      getTasksByGroup: vi.fn(), getTasksByGroupAndUser: vi.fn(), getMyTasks: vi.fn(), getTaskById: vi.fn(), createTask: vi.fn(),
      updateTask: vi.fn(), updateTaskStatus: vi.fn(), deleteTask: vi.fn(),
    };
    isSignedIn = signal(true);
    TestBed.configureTestingModule({
      providers: [{ provide: TasksApi, useValue: api }, { provide: IamStore, useValue: { isSignedIn } }],
    });
    store = TestBed.inject(TasksStore);
    toasts = TestBed.inject(ToastStore);
  });

  const loadGroup = (...tasks: Task[]) => {
    api.getTasksByGroup.mockReturnValue(of(tasks));
    store.loadTasksByGroup(10);
  };

  it('carga las tareas de un grupo y pasa el filtro de estado al backend', () => {
    loadGroup(task(1), task(2, 'DONE'));
    expect(store.taskCount()).toBe(2);
    store.loadTasksByGroup(10, 'DONE');
    expect(api.getTasksByGroup).toHaveBeenLastCalledWith(10, 'DONE');
  });

  it('filteredTasks aplica el filtro local', () => {
    loadGroup(task(1), task(2, 'DONE'), task(3, 'DONE'));
    store.setStatusFilter('DONE');
    expect(store.filteredTasks().map((t) => t.id)).toEqual([2, 3]);
    store.setStatusFilter('ALL');
    expect(store.filteredTasks()).toHaveLength(3);
  });

  it('un error al cargar queda en error y en un toast', () => {
    api.getTasksByGroup.mockReturnValue(throwError(() => new Error('Failed to fetch tasks for group 10: 403')));
    store.loadTasksByGroup(10);
    expect(store.error()).toContain('403');
    expect(toasts.toasts()[0].message).toBe('No tienes permisos para realizar esta acción.');
    expect(store.loading()).toBe(false);
  });

  describe('AUDIT PERF-03: mis tareas con una sola petición', () => {
    it('carga mis tareas y los grupos donde tengo alguna', () => {
      const mine = [task(2, 'IN_PROGRESS', '2026-12-01T00:00:00Z'), task(1, 'IN_PROGRESS', '2026-12-10T00:00:00Z')];
      api.getMyTasks.mockReturnValue(of(mine));
      store.loadMyTasks();
      expect(api.getMyTasks).toHaveBeenCalledTimes(1);
      expect(store.tasks().map((t) => t.id)).toEqual([2, 1]);
      expect(store.assignedGroupIds()).toEqual([...new Set(mine.map((t) => t.group?.id))].filter((id) => id != null));
      expect(store.error()).toBeNull();
      expect(store.loading()).toBe(false);
    });

    it('si falla avisa y deja de cargar', () => {
      api.getMyTasks.mockReturnValue(throwError(() => new Error('Failed to load your tasks: 500')));
      store.loadMyTasks();
      expect(store.error()).toBe('Failed to load your tasks: 500');
      expect(toasts.toasts()[0].message).toBe('El servidor tuvo un problema. Inténtalo más tarde.');
      expect(store.loading()).toBe(false);
    });
  });

  it('loadTaskById selecciona la tarea y la agrega o reemplaza en la lista', () => {
    loadGroup(task(1));
    api.getTaskById.mockReturnValueOnce(of(task(1, 'ON_HOLD'))).mockReturnValueOnce(of(task(9)));
    store.loadTaskById(1);
    store.loadTaskById(9);
    expect(store.tasks().map((t) => [t.id, t.status])).toEqual([[1, 'ON_HOLD'], [9, 'IN_PROGRESS']]);
    expect(store.selectedTask()?.id).toBe(9);
    expect(store.getTaskById(1)()?.status).toBe('ON_HOLD');
  });

  it('crear, cambiar estado, editar y borrar actualizan la lista', () => {
    loadGroup(task(1));
    api.createTask.mockReturnValue(of(task(2)));
    api.updateTaskStatus.mockReturnValue(of(task(1, 'COMPLETED')));
    api.updateTask.mockReturnValue(of(new Task({ ...{ id: 2, title: 'Editada', description: '', dueDate: new Date(), createdAt: new Date(), updatedAt: new Date(), status: 'IN_PROGRESS' } })));
    api.deleteTask.mockReturnValue(of(undefined));

    store.addTask(new CreateTaskCommand('T2', '', new Date(), 5, 10));
    store.updateTaskStatus(1, 'COMPLETED');
    store.updateTask(2, new UpdateTaskCommand('Editada', '', new Date(), 5));
    store.deleteTask(1);

    expect(store.tasks().map((t) => [t.id, t.title])).toEqual([[2, 'Editada']]);
  });

  it('una tarea DONE no se edita ni se borra', () => {
    loadGroup(task(1, 'DONE'));
    store.updateTask(1, new UpdateTaskCommand('x', '', new Date(), 5));
    store.deleteTask(1);
    expect(api.updateTask).not.toHaveBeenCalled();
    expect(api.deleteTask).not.toHaveBeenCalled();
    expect(store.error()).toBe('Una tarea terminada no puede editarse ni eliminarse.');
    expect(store.tasks()).toHaveLength(1);
  });

  it('al cerrar sesión se borran las tareas y el filtro', () => {
    loadGroup(task(1));
    store.setStatusFilter('DONE');
    isSignedIn.set(false);
    TestBed.tick();
    expect(store.tasks()).toEqual([]);
    expect(store.statusFilter()).toBe('ALL');
  });
});
