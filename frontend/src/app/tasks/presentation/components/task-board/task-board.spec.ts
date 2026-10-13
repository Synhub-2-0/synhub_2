import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TaskBoard } from './task-board';
import { Task, TaskStatus } from '../../../domain/model/task.entity';

const task = (id: number, status: TaskStatus, assigneeId = 5) => new Task({
  id, title: `T${id}`, description: '', dueDate: new Date(), createdAt: new Date(), updatedAt: new Date(), status,
  assignedTo: { id: assigneeId, username: 'u', name: 'Ana', surname: 'Torres', imgUrl: '', email: '', accountRoles: [] },
});

describe('TaskBoard', () => {
  let fixture: ComponentFixture<TaskBoard>;
  let board: TaskBoard;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TaskBoard] }).compileComponents();
    fixture = TestBed.createComponent(TaskBoard);
    board = fixture.componentInstance;
    fixture.componentRef.setInput('tasks', [task(1, 'IN_PROGRESS'), task(2, 'DONE'), task(3, 'IN_PROGRESS', 6), task(4, 'EXPIRED')]);
    fixture.componentRef.setInput('currentUserId', 5);
    await fixture.whenStable();
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

  it('el miembro solo mueve sus tareas; el líder todas; nadie mueve una DONE', () => {
    expect(board.canChangeStatus(task(1, 'IN_PROGRESS', 5))).toBe(true);
    expect(board.canChangeStatus(task(3, 'IN_PROGRESS', 6))).toBe(false);
    expect(board.canChangeStatus(task(2, 'DONE', 5))).toBe(false);
    fixture.componentRef.setInput('canManageAll', true);
    expect(board.canChangeStatus(task(3, 'IN_PROGRESS', 6))).toBe(true);
    expect(board.canChangeStatus(task(2, 'DONE', 6))).toBe(false);
  });

  it('emite statusChange al elegir un estado', () => {
    const emitted: unknown[] = [];
    board.statusChange.subscribe((e) => emitted.push(e));
    const select = document.createElement('select');
    select.innerHTML = '<option value="COMPLETED" selected>Completada</option>';
    board.onStatusChange(1, { target: select } as unknown as Event);
    expect(emitted).toEqual([{ taskId: 1, status: 'COMPLETED' }]);
  });

  it('etiquetas e iniciales', () => {
    expect(board.statusLabel('ON_HOLD')).toBe('En espera');
    expect(board.initialsOf(task(1, 'IN_PROGRESS'))).toBe('AT');
  });
});
