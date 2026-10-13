import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { TaskCard } from './task-card';
import { Task, TaskStatus } from '../../../domain/model/task.entity';
import { RequestsApi } from '../../../../requests/infrastructure/requests.api';
import { RequestsStore } from '../../../../requests/application/requests.store';
import { IamStore } from '../../../../iam/application/iam.store';
import { TaskRequest } from '../../../../requests/domain/model/task-request.entity';

const task = (status: TaskStatus, dueDate = new Date(Date.now() + 2 * 86_400_000 + 3_600_000)) => new Task({
  id: 7, title: 'Login', description: '', dueDate, createdAt: new Date(), updatedAt: new Date(), status,
  assignedTo: { id: 5, username: 'ana', name: 'Ana', surname: 'Torres', imgUrl: '', email: '', accountRoles: [] },
});

describe('TaskCard', () => {
  let fixture: ComponentFixture<TaskCard>;
  let card: TaskCard;
  let api: Record<'getRequestsByTask' | 'createRequest', ReturnType<typeof vi.fn>>;

  const render = async (status: TaskStatus, isLeader = false) => {
    fixture = TestBed.createComponent(TaskCard);
    card = fixture.componentInstance;
    card.task = task(status);
    card.isLeader = isLeader;
    fixture.detectChanges();
    await fixture.whenStable();
  };

  beforeEach(async () => {
    api = { getRequestsByTask: vi.fn().mockReturnValue(of([])), createRequest: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [TaskCard],
      providers: [
        provideRouter([]),
        { provide: RequestsApi, useValue: api },
        { provide: IamStore, useValue: { isSignedIn: signal(true) } },
      ],
    }).compileComponents();
  });

  it('el miembro con una tarea COMPLETED consulta si ya hay una entrega en revisión', async () => {
    const pending = new TaskRequest({ id: 1, description: '', requestType: 'SUBMISSION', requestStatus: 'PENDING', task: task('COMPLETED') });
    api.getRequestsByTask.mockReturnValue(of([pending]));
    await render('COMPLETED');
    expect(api.getRequestsByTask).toHaveBeenCalledWith(7);
    expect(TestBed.inject(RequestsStore).isUnderReview(7)).toBe(true);
  });

  it('el líder o una tarea no completada no consultan entregas', async () => {
    await render('COMPLETED', true);
    await render('IN_PROGRESS');
    expect(api.getRequestsByTask).not.toHaveBeenCalled();
  });

  it('enviar la entrega usa el mensaje sin espacios y cierra el formulario', async () => {
    await render('COMPLETED');
    api.createRequest.mockReturnValue(of(new TaskRequest({ id: 2, description: 'Listo', requestType: 'SUBMISSION', requestStatus: 'PENDING', task: task('COMPLETED') })));
    card.showSubmitForm.set(true);
    card.submissionMessage.set('  Listo  ');

    card.onSubmit(new Event('click'));

    expect(api.createRequest.mock.calls[0][0].description).toBe('Listo');
    expect(card.showSubmitForm()).toBe(false);
    expect(card.submissionMessage()).toBe('');
  });

  it('una tarea DONE queda bloqueada; etiquetas y tiempo restante', async () => {
    await render('DONE');
    expect(card.isLocked).toBe(true);
    expect(card.statusLabel).toBe('Terminada');
    expect(card.initials).toBe('AT');
    card.task = task('IN_PROGRESS');
    expect(card.remainingTimeLabel).toMatch(/^2d \d+h restantes$/);
    card.task = task('IN_PROGRESS', new Date(Date.now() - 1000));
    expect(card.remainingTimeLabel).toBe('Plazo vencido');
  });

  it('borrar y cambiar estado se emiten al padre', async () => {
    await render('IN_PROGRESS', true);
    const deleted: number[] = [];
    const changed: unknown[] = [];
    card.deleteRequested.subscribe((id) => deleted.push(id));
    card.statusChangeRequested.subscribe((e) => changed.push(e));
    card.onDelete(new Event('click'));
    card.onChangeStatus(new Event('click'), 'ON_HOLD');
    expect(deleted).toEqual([7]);
    expect(changed).toEqual([{ taskId: 7, status: 'ON_HOLD' }]);
  });
});
