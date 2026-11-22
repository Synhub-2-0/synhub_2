import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TaskBoard } from './task-board';
import { Task, TaskStatus } from '../../../domain/model/task.entity';

const task = (id: number, status: TaskStatus, assigneeId = 5) => new Task({
  id, title: `T${id}`, description: '', dueDate: new Date(), createdAt: new Date(), updatedAt: new Date(), status,
  assignedTo: { id: assigneeId, username: 'u', name: 'Ana', surname: 'Torres', imgUrl: '', email: '' },
});

describe('TaskBoard', () => {
  let fixture: ComponentFixture<TaskBoard>;
  let board: TaskBoard;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TaskBoard] }).compileComponents();
    fixture = TestBed.createComponent(TaskBoard);
    board = fixture.componentInstance;
    fixture.componentRef.setInput('tasks', [task(1, 'IN_PROGRESS'), task(2, 'DONE'), task(3, 'IN_PROGRESS', 6), task(4, 'EXPIRED')]);
    await fixture.whenStable();
  });

  const select = (status: TaskStatus) => {
    const element = document.createElement('select');
    for (const value of ['IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'EXPIRED']) element.add(new Option(value, value));
    element.value = status;
    return { target: element } as unknown as Event;
  };

  it('cambiar el estado de una tarea en progreso se emite directo', () => {
    const emitted: { taskId: number; status: TaskStatus }[] = [];
    board.statusChange.subscribe((e) => emitted.push(e));
    board.onStatusChange(task(1, 'IN_PROGRESS'), select('COMPLETED'));
    expect(emitted).toEqual([{ taskId: 1, status: 'COMPLETED' }]);
    expect(board.resolving()).toBeNull();
  });

  it('cambiar el estado de una tarea vencida abre el diálogo y no emite nada', () => {
    const emitted: unknown[] = [];
    board.statusChange.subscribe((e) => emitted.push(e));
    const event = select('IN_PROGRESS');

    board.onStatusChange(task(4, 'EXPIRED'), event);

    expect(board.resolving()?.task.id).toBe(4);
    expect(emitted).toEqual([]);
    expect((event.target as HTMLSelectElement).value).toBe('EXPIRED'); // el selector vuelve a su valor
  });

  it('cambiar el estado de una tarea en espera abre el diálogo; si no hay nada que resolver sigue el cambio normal', () => {
    const emitted: { taskId: number; status: TaskStatus }[] = [];
    board.statusChange.subscribe((e) => emitted.push(e));

    board.onStatusChange(task(5, 'ON_HOLD'), select('IN_PROGRESS'));
    expect(board.resolving()?.task.id).toBe(5);

    board.proceedResolving();
    expect(emitted).toEqual([{ taskId: 5, status: 'IN_PROGRESS' }]);
    expect(board.resolving()).toBeNull();
  });

  it('cerrar el diálogo lo quita sin emitir nada', () => {
    const emitted: unknown[] = [];
    board.statusChange.subscribe((e) => emitted.push(e));
    board.onStatusChange(task(4, 'EXPIRED'), select('IN_PROGRESS'));
    board.finishResolving();
    expect(board.resolving()).toBeNull();
    expect(emitted).toEqual([]);
  });

  it('en un grupo bloqueado el líder no cambia estados desde el tablero', () => {
    fixture.componentRef.setInput('canManageAll', true);
    const locked = new Task({
      id: 9, title: 'T9', description: '', dueDate: new Date(), createdAt: new Date(), updatedAt: new Date(), status: 'IN_PROGRESS',
      group: { id: 3, name: 'G', imgUrl: '', description: '', code: 'C', memberCount: 1, locked: true, leader: { id: 1, username: 'lider', name: 'Lia', surname: 'D', imgUrl: '', email: '' } },
    });
    expect(board.canChangeStatus(task(1, 'IN_PROGRESS'))).toBe(true);
    expect(board.canChangeStatus(locked)).toBe(false);
  });

  it('agrupa las tareas por columna de estado', () => {
    const byStatus = board.tasksByStatus();
    expect(byStatus.IN_PROGRESS.map((t) => t.id)).toEqual([1, 3]);
    expect(byStatus.DONE.map((t) => t.id)).toEqual([2]);
    expect(byStatus.EXPIRED.map((t) => t.id)).toEqual([4]);
    expect(byStatus.ON_HOLD).toEqual([]);
  });

  it('DONE no se ofrece como estado seleccionable', () => {
    expect(board.selectableStatuses.map((s) => s.value)).not.toContain('DONE');
  });

  it('solo el líder mueve tareas, ni siquiera las propias del miembro; nadie mueve una DONE', () => {
    expect(board.canChangeStatus(task(1, 'IN_PROGRESS', 5))).toBe(false);
    expect(board.canChangeStatus(task(3, 'ON_HOLD', 6))).toBe(false);
    expect(board.canChangeStatus(task(5, 'COMPLETED', 5))).toBe(false);
    fixture.componentRef.setInput('canManageAll', true);
    expect(board.canChangeStatus(task(3, 'IN_PROGRESS', 6))).toBe(true);
    expect(board.canChangeStatus(task(5, 'COMPLETED', 5))).toBe(true);
    expect(board.canChangeStatus(task(2, 'DONE', 6))).toBe(false);
  });

  it('emite statusChange al elegir un estado', () => {
    const emitted: unknown[] = [];
    board.statusChange.subscribe((e) => emitted.push(e));
    const select = document.createElement('select');
    select.innerHTML = '<option value="COMPLETED" selected>Completada</option>';
    board.onStatusChange(task(1, 'IN_PROGRESS'), { target: select } as unknown as Event);
    expect(emitted).toEqual([{ taskId: 1, status: 'COMPLETED' }]);
  });

  it('etiquetas e iniciales', () => {
    expect(board.statusLabel('ON_HOLD')).toBe('En espera');
    expect(board.initialsOf(task(1, 'IN_PROGRESS'))).toBe('AT');
  });
});
