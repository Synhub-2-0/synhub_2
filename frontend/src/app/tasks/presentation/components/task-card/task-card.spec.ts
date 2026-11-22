import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Router, provideRouter } from '@angular/router';
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

  it('una tarea en espera del miembro consulta si tiene una modificación pendiente', async () => {
    const pending = new TaskRequest({ id: 1, description: 'Cambiar', requestType: 'MODIFICATION', requestStatus: 'PENDING', task: task('ON_HOLD') });
    api.getRequestsByTask.mockReturnValue(of([pending]));
    await render('ON_HOLD');
    expect(api.getRequestsByTask).toHaveBeenCalledWith(7);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Modificación pendiente: el líder debe editar la tarea');
    expect(text).not.toContain('Solo el líder del grupo puede cambiar su estado');
  });

  it('el líder o una tarea en progreso no consultan solicitudes', async () => {
    await render('COMPLETED', true);
    await render('IN_PROGRESS');
    expect(api.getRequestsByTask).not.toHaveBeenCalled();
  });

  it('el nombre del grupo abre la pantalla del grupo sin abrir la tarea', async () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(TaskCard);
    card = fixture.componentInstance;
    card.task = new Task({
      id: 7, title: 'Login', description: '', dueDate: new Date(), createdAt: new Date(), updatedAt: new Date(), status: 'IN_PROGRESS',
      group: { id: 3, name: 'Diseño UX', imgUrl: '', description: '', code: 'ABC123', memberCount: 4, leader: { id: 1, username: 'lider', name: 'Lia', surname: 'D', imgUrl: '', email: '' } },
    });
    card.showGroup = true;
    fixture.detectChanges();
    await fixture.whenStable();

    const button = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'))
      .find((b) => b.textContent?.includes('Diseño UX')) as HTMLButtonElement;
    expect(button.textContent).toContain('Diseño UX');
    button.click();

    expect(navigate).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith(['/groups', 3]);
  });

  it('el miembro ve las acciones de su tarea en la tarjeta', async () => {
    await render('IN_PROGRESS');
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Marcar completada y enviar a validación');
    expect(text).toContain('Solicitar modificación');
  });

  it('el líder de un grupo bloqueado no ve editar, eliminar ni revisar', async () => {
    fixture = TestBed.createComponent(TaskCard);
    card = fixture.componentInstance;
    card.task = new Task({
      id: 7, title: 'Login', description: '', dueDate: new Date(Date.now() + 86_400_000), createdAt: new Date(), updatedAt: new Date(), status: 'COMPLETED',
      group: { id: 3, name: 'G', imgUrl: '', description: '', code: 'C', memberCount: 2, locked: true, leader: { id: 1, username: 'lider', name: 'Lia', surname: 'D', imgUrl: '', email: '' } },
    });
    card.isLeader = true;
    fixture.detectChanges();
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('Editar');
    expect(text).not.toContain('Eliminar');
    expect(text).not.toContain('Revisar entrega');
  });

  it('el líder no ve el botón de solicitar modificación', async () => {
    await render('IN_PROGRESS', true);
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Solicitar modificación');
  });

  it('no hay formulario ni campo de comentario', async () => {
    await render('IN_PROGRESS');
    expect((fixture.nativeElement as HTMLElement).querySelector('textarea')).toBeNull();
  });

  it('una tarea COMPLETED sin entrega pendiente no ofrece completar de nuevo', async () => {
    await render('COMPLETED');
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).not.toContain('Marcar completada');
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
