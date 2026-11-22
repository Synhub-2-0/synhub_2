import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { TaskMemberActions } from './task-member-actions';
import { Task, TaskStatus } from '../../../domain/model/task.entity';
import { RequestsApi } from '../../../../requests/infrastructure/requests.api';
import { RequestsStore } from '../../../../requests/application/requests.store';
import { TasksStore } from '../../../application/tasks.store';
import { IamStore } from '../../../../iam/application/iam.store';
import { TaskRequest } from '../../../../requests/domain/model/task-request.entity';

const task = (status: TaskStatus) => new Task({
  id: 7, title: 'Login', description: '', dueDate: new Date(Date.now() + 86_400_000), createdAt: new Date(), updatedAt: new Date(), status,
});

const lockedTask = (status: TaskStatus) => new Task({
  id: 7, title: 'Login', description: '', dueDate: new Date(Date.now() + 86_400_000), createdAt: new Date(), updatedAt: new Date(), status,
  group: { id: 3, name: 'G', imgUrl: '', description: '', code: 'C', memberCount: 2, locked: true, leader: { id: 1, username: 'lider', name: 'Lia', surname: 'D', imgUrl: '', email: '' } },
});

describe('TaskMemberActions', () => {
  let fixture: ComponentFixture<TaskMemberActions>;
  let component: TaskMemberActions;
  let api: Record<'getRequestsByTask' | 'createRequest', ReturnType<typeof vi.fn>>;
  let tasksStore: { applyUpdatedTask: ReturnType<typeof vi.fn> };

  const render = async (status: TaskStatus, locked = false) => {
    fixture = TestBed.createComponent(TaskMemberActions);
    component = fixture.componentInstance;
    component.task = locked ? lockedTask(status) : task(status);
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const text = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  beforeEach(async () => {
    api = { getRequestsByTask: vi.fn().mockReturnValue(of([])), createRequest: vi.fn() };
    tasksStore = { applyUpdatedTask: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [TaskMemberActions],
      providers: [
        { provide: RequestsApi, useValue: api },
        { provide: TasksStore, useValue: tasksStore },
        { provide: IamStore, useValue: { isSignedIn: signal(true) } },
      ],
    }).compileComponents();
  });

  it('una tarea COMPLETED consulta si ya hay una entrega en revisión y la muestra', async () => {
    const pending = new TaskRequest({ id: 1, description: '', requestType: 'SUBMISSION', requestStatus: 'PENDING', task: task('COMPLETED') });
    api.getRequestsByTask.mockReturnValue(of([pending]));
    await render('COMPLETED');
    expect(api.getRequestsByTask).toHaveBeenCalledWith(7);
    expect(text()).toContain('En revisión por el líder');
  });

  it('una tarea en espera consulta si tiene una modificación pendiente', async () => {
    const pending = new TaskRequest({ id: 1, description: 'Cambiar', requestType: 'MODIFICATION', requestStatus: 'PENDING', task: task('ON_HOLD') });
    api.getRequestsByTask.mockReturnValue(of([pending]));
    await render('ON_HOLD');
    expect(api.getRequestsByTask).toHaveBeenCalledWith(7);
    expect(text()).toContain('Modificación pendiente: el líder debe editar la tarea');
    expect(text()).not.toContain('Solo el líder del grupo puede cambiar su estado');
  });

  it('una tarea en progreso no consulta solicitudes', async () => {
    await render('IN_PROGRESS');
    expect(api.getRequestsByTask).not.toHaveBeenCalled();
  });

  it('completar una tarea en progreso la envía a validación sin pedir comentario y actualiza la tarea', async () => {
    await render('IN_PROGRESS');
    api.createRequest.mockReturnValue(of(new TaskRequest({ id: 2, description: 'Tarea completada.', requestType: 'SUBMISSION', requestStatus: 'PENDING', task: task('COMPLETED') })));

    component.onComplete(new Event('click'));

    expect(api.createRequest.mock.calls[0][0].taskId).toBe(7);
    expect(tasksStore.applyUpdatedTask).toHaveBeenCalledWith(expect.objectContaining({ id: 7, status: 'COMPLETED' }));
    expect(TestBed.inject(RequestsStore).isUnderReview(7)).toBe(true);
  });

  it('solicitar una modificación exige comentario, deja la tarea en espera y la marca pendiente', async () => {
    await render('IN_PROGRESS');
    expect(text()).toContain('Solicitar modificación');

    component.toggleModificationForm(new Event('click'));
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('textarea')).not.toBeNull();

    component.onRequestModification(new Event('click'));
    expect(api.createRequest).not.toHaveBeenCalled();

    api.createRequest.mockReturnValue(of(new TaskRequest({ id: 3, description: 'Cambiar el alcance', requestType: 'MODIFICATION', requestStatus: 'PENDING', task: task('ON_HOLD') })));
    component.modificationComment.set('  Cambiar el alcance  ');
    component.onRequestModification(new Event('click'));

    const command = api.createRequest.mock.calls[0][0];
    expect([command.taskId, command.description, command.requestType]).toEqual([7, 'Cambiar el alcance', 'MODIFICATION']);
    expect(tasksStore.applyUpdatedTask).toHaveBeenCalledWith(expect.objectContaining({ id: 7, status: 'ON_HOLD' }));
    expect(TestBed.inject(RequestsStore).hasPendingModification(7)).toBe(true);
    expect(component.showModificationForm()).toBe(false);
    expect(component.modificationComment()).toBe('');
  });

  it('el botón de solicitar modificación es amarillo para destacar que es una opción', async () => {
    await render('IN_PROGRESS');
    const button = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'))
      .find((b) => b.textContent?.includes('Solicitar modificación'));
    expect(button?.className).toContain('bg-amber-400');
  });

  it('no hay campo de comentario hasta que se pide la modificación', async () => {
    await render('IN_PROGRESS');
    expect((fixture.nativeElement as HTMLElement).querySelector('textarea')).toBeNull();
  });

  it('una tarea COMPLETED sin entrega pendiente no ofrece completar de nuevo', async () => {
    await render('COMPLETED');
    expect(text()).not.toContain('Marcar completada');
  });

  it('en espera sin solicitud y vencida solo avisan que cambia el líder', async () => {
    for (const status of ['ON_HOLD', 'EXPIRED'] as TaskStatus[]) {
      await render(status);
      expect(text()).toContain('Solo el líder del grupo puede cambiar su estado');
      expect(text()).not.toContain('Marcar completada');
    }
  });

  it('en un grupo bloqueado no se completa ni se pide modificación: solo se avisa', async () => {
    await render('IN_PROGRESS', true);
    expect(text()).toContain('Grupo bloqueado');
    expect(text()).not.toContain('Marcar completada');
    expect(text()).not.toContain('Solicitar modificación');
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('button').length).toBe(0);
  });

  it('una tarea terminada no ofrece nada', async () => {
    await render('DONE');
    expect(text().trim()).toBe('');
  });
});
