import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { TaskCard } from './task-card';
import { Task, TaskStatus } from '../../../domain/model/task.entity';
import { RequestsApi } from '../../../../requests/infrastructure/requests.api';
import { RequestsStore } from '../../../../requests/application/requests.store';
import { TasksStore } from '../../../application/tasks.store';
import { IamStore } from '../../../../iam/application/iam.store';
import { TaskRequest } from '../../../../requests/domain/model/task-request.entity';

const task = (status: TaskStatus, dueDate = new Date(Date.now() + 2 * 86_400_000 + 3_600_000)) => new Task({
  id: 7, title: 'Login', description: '', dueDate, createdAt: new Date(), updatedAt: new Date(), status,
  assignedTo: { id: 5, username: 'ana', name: 'Ana', surname: 'Torres', imgUrl: '', email: '' },
});

describe('TaskCard', () => {
  let fixture: ComponentFixture<TaskCard>;
  let card: TaskCard;
  let api: Record<'getRequestsByTask' | 'createRequest', ReturnType<typeof vi.fn>>;
  let tasksStore: { applyUpdatedTask: ReturnType<typeof vi.fn> };

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
    tasksStore = { applyUpdatedTask: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [TaskCard],
      providers: [
        provideRouter([]),
        { provide: RequestsApi, useValue: api },
        { provide: TasksStore, useValue: tasksStore },
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
    expect(tasksStore.applyUpdatedTask).toHaveBeenCalledWith(expect.objectContaining({ id: 7 }));
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

  it('borrar se emite al padre', async () => {
    await render('IN_PROGRESS', true);
    const deleted: number[] = [];
    card.deleteRequested.subscribe((id) => deleted.push(id));
    card.onDelete(new Event('click'));
    expect(deleted).toEqual([7]);
  });

  it('el miembro completa una tarea en progreso enviándola a validación, sin botones de estado', async () => {
    await render('IN_PROGRESS');
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Marcar completada y enviar a validación');
    expect(text).not.toContain('Poner en Espera');
  });

  it('el miembro no puede cambiar el estado de una tarea en espera ni vencida', async () => {
    for (const status of ['ON_HOLD', 'EXPIRED'] as TaskStatus[]) {
      await render(status);
      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('Solo el líder del grupo puede cambiar su estado');
      expect(text).not.toContain('Marcar completada');
    }
  });
});
