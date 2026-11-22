import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Router, provideRouter } from '@angular/router';
import { TaskResolveDialog } from './task-resolve-dialog';
import { Task, TaskStatus } from '../../../domain/model/task.entity';
import { TaskRequest } from '../../../../requests/domain/model/task-request.entity';
import { RequestsStore } from '../../../../requests/application/requests.store';
import { TasksStore } from '../../../application/tasks.store';

const task = (status: TaskStatus) => new Task({
  id: 7, title: 'Login', description: '', dueDate: new Date(Date.now() - 86_400_000), createdAt: new Date(), updatedAt: new Date(), status,
  assignedTo: { id: 5, username: 'ana', name: 'Ana', surname: 'Torres', imgUrl: '', email: '' },
  group: { id: 3, name: 'G', imgUrl: '', description: '', code: 'C', memberCount: 2, leader: { id: 1, username: 'lider', name: 'Lia', surname: 'D', imgUrl: '', email: '' } },
});

const modification = (status: TaskStatus = 'ON_HOLD') => new TaskRequest({
  id: 40, description: 'Cambiar el alcance', requestType: 'MODIFICATION', requestStatus: 'PENDING', task: task(status),
});

describe('TaskResolveDialog', () => {
  let fixture: ComponentFixture<TaskResolveDialog>;
  let dialog: TaskResolveDialog;
  let requestsByTask: ReturnType<typeof signal<Record<number, TaskRequest[]>>>;
  let loadTaskRequests: ReturnType<typeof vi.fn>;
  let rejectRequest: ReturnType<typeof vi.fn>;
  let resumeExpiredTask: ReturnType<typeof vi.fn>;
  let applyUpdatedTask: ReturnType<typeof vi.fn>;
  let closed: ReturnType<typeof vi.fn>;
  let proceed: ReturnType<typeof vi.fn>;

  /** pending: lo que devuelve la consulta de solicitudes de la tarea (se carga al abrir un diálogo de una tarea en espera). */
  const open = async (status: TaskStatus, pending: TaskRequest[] = []) => {
    TestBed.resetTestingModule();
    requestsByTask = signal<Record<number, TaskRequest[]>>({});
    loadTaskRequests = vi.fn((taskId: number, done?: () => void) => {
      requestsByTask.set({ [taskId]: pending });
      done?.();
    });
    rejectRequest = vi.fn();
    resumeExpiredTask = vi.fn();
    applyUpdatedTask = vi.fn();
    closed = vi.fn();
    proceed = vi.fn();
    await TestBed.configureTestingModule({
      imports: [TaskResolveDialog],
      providers: [
        provideRouter([]),
        { provide: RequestsStore, useValue: { requestsByTask, loadTaskRequests, rejectRequest, processing: signal(false) } },
        { provide: TasksStore, useValue: { resumeExpiredTask, applyUpdatedTask, loading: signal(false) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(TaskResolveDialog);
    dialog = fixture.componentInstance;
    fixture.componentRef.setInput('task', task(status));
    dialog.closed.subscribe(closed);
    dialog.proceed.subscribe(proceed);
    fixture.detectChanges();
    await fixture.whenStable();
  };

  const text = () => (fixture.nativeElement as HTMLElement).textContent ?? '';
  const button = (label: string) =>
    Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).find((b) => b.textContent?.includes(label));

  describe('tarea vencida', () => {
    beforeEach(() => open('EXPIRED'));

    it('pide una nueva fecha límite y no consulta solicitudes', () => {
      expect(text()).toContain('venció');
      expect(text()).toContain('nueva fecha límite');
      expect(loadTaskRequests).not.toHaveBeenCalled();
    });

    it('reanudar exige una fecha futura', () => {
      expect((button('Reanudar tarea') as HTMLButtonElement).disabled).toBe(true);
      dialog.newDueDate.set('2020-01-01T10:00');
      expect(dialog.isDateValid()).toBe(false);
      dialog.resume();
      expect(resumeExpiredTask).not.toHaveBeenCalled();
    });

    it('con una fecha futura reanuda la tarea y se cierra al terminar', () => {
      const future = new Date(Date.now() + 2 * 86_400_000);
      dialog.newDueDate.set(new Date(future.getTime() - future.getTimezoneOffset() * 60_000).toISOString().slice(0, 16));
      expect(dialog.isDateValid()).toBe(true);

      dialog.resume();

      expect(resumeExpiredTask).toHaveBeenCalledWith(expect.objectContaining({ id: 7 }), expect.any(Date), expect.any(Function));
      expect(closed).not.toHaveBeenCalled();
      resumeExpiredTask.mock.calls[0][2]();
      expect(closed).toHaveBeenCalled();
    });

    it('cancelar cierra sin cambiar nada', () => {
      dialog.cancel();
      expect(closed).toHaveBeenCalled();
      expect(resumeExpiredTask).not.toHaveBeenCalled();
    });
  });

  describe('tarea en espera', () => {
    it('sin una modificación pendiente deja seguir con el cambio de estado normal', async () => {
      await open('ON_HOLD', []);
      expect(loadTaskRequests).toHaveBeenCalledWith(7, expect.any(Function));
      expect(proceed).toHaveBeenCalled();
    });

    it('con una modificación pendiente muestra el comentario y las dos salidas', async () => {
      await open('ON_HOLD', [modification()]);
      expect(proceed).not.toHaveBeenCalled();
      expect(text()).toContain('Cambiar el alcance');
      expect(button('Editar tarea')).toBeDefined();
      expect(button('Rechazar solicitud')).toBeDefined();
    });

    it('rechazar la solicitud reanuda la tarea y actualiza la lista', async () => {
      await open('ON_HOLD', [modification()]);

      dialog.deny();

      expect(rejectRequest).toHaveBeenCalledWith(expect.objectContaining({ id: 40 }), expect.any(Function));
      rejectRequest.mock.calls[0][1](modification('IN_PROGRESS'));
      expect(applyUpdatedTask).toHaveBeenCalledWith(expect.objectContaining({ id: 7, status: 'IN_PROGRESS' }));
      expect(closed).toHaveBeenCalled();
    });

    it('editar la tarea abre su formulario y vuelve a la pantalla actual', async () => {
      await open('ON_HOLD', [modification()]);
      const router = TestBed.inject(Router);
      const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

      dialog.edit();

      expect(navigate).toHaveBeenCalledWith(['/tasks', 7, 'edit'], { queryParams: { returnTo: router.url } });
    });

    it('cancelar cierra sin cambiar nada', async () => {
      await open('ON_HOLD', [modification()]);
      dialog.cancel();
      expect(closed).toHaveBeenCalled();
      expect(rejectRequest).not.toHaveBeenCalled();
    });
  });
});
