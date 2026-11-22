import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Validations } from './validations';
import { RequestsApi } from '../../../infrastructure/requests.api';
import { RequestsStore } from '../../../application/requests.store';
import { TasksStore } from '../../../../tasks/application/tasks.store';
import { GroupsApi } from '../../../../groups/infrastructure/groups.api';
import { GroupsStore } from '../../../../groups/application/groups.store';
import { IamApi } from '../../../../iam/infrastructure/iam.api';
import { IamStore } from '../../../../iam/application/iam.store';
import { TaskRequest } from '../../../domain/model/task-request.entity';
import { Task } from '../../../../tasks/domain/model/task.entity';

const apiStub = () => new Proxy({} as Record<string, ReturnType<typeof vi.fn>>, {
  get: (target, prop: string) => (target[prop] ??= vi.fn(() => of([]))),
});

const task = (overdue = false) => new Task({
  id: 7, title: 'T', description: '', status: 'COMPLETED', createdAt: new Date(), updatedAt: new Date(),
  dueDate: overdue ? new Date(Date.now() - 86_400_000) : new Date(Date.now() + 86_400_000),
  assignedTo: { id: 5, username: 'ana', name: 'Ana', surname: 'Torres', imgUrl: '', email: '' },
});

const request = (id: number, overdue = false, requestType: 'SUBMISSION' | 'MODIFICATION' | 'EXPIRED' = 'SUBMISSION') => new TaskRequest({
  id, description: 'Listo', requestType, requestStatus: 'PENDING', task: task(overdue),
});

describe('Validations', () => {
  let approveRequest: ReturnType<typeof vi.fn>;
  let rejectRequest: ReturnType<typeof vi.fn>;
  let loadGroupRequests: ReturnType<typeof vi.fn>;
  let resumeExpiredTask: ReturnType<typeof vi.fn>;
  let component: Validations;

  beforeEach(async () => {
    localStorage.clear();
    approveRequest = vi.fn();
    rejectRequest = vi.fn();
    loadGroupRequests = vi.fn();
    resumeExpiredTask = vi.fn();
    await TestBed.configureTestingModule({
      imports: [Validations],
      providers: [
        provideRouter([]),
        { provide: RequestsApi, useValue: apiStub() },
        { provide: GroupsApi, useValue: apiStub() },
        { provide: IamApi, useValue: apiStub() },
        { provide: IamStore, useValue: { isSignedIn: signal(true) } },
        {
          provide: RequestsStore,
          useValue: {
            approveRequest, rejectRequest, loadGroupRequests,
            groupRequests: signal([]), processing: signal(false),
            statusFilter: signal('PENDING'), typeFilter: signal('SUBMISSION'),
          },
        },
        { provide: TasksStore, useValue: { resumeExpiredTask } },
        {
          provide: GroupsStore,
          useValue: {
            loadGroups: vi.fn(), leaderGroups: signal([]), memberGroups: signal([]),
            groups: signal([]), loading: signal(false),
          },
        },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({}) } } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(Validations);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('se crea y ofrece los filtros de estado y de tipo', () => {
    expect(component).toBeTruthy();
    expect(component.statusOptions.map((o) => o.value)).toEqual(['PENDING', 'APPROVED', 'REJECTED']);
    expect(component.typeOptions.map((o) => o.value)).toEqual(['SUBMISSION', 'MODIFICATION', 'EXPIRED']);
  });

  it('cambiar el tipo recarga la bandeja del grupo elegido con ese tipo', () => {
    component.selectedGroupId.set(10);
    component.setType('MODIFICATION');
    expect(loadGroupRequests).toHaveBeenLastCalledWith(10, undefined, 'MODIFICATION');
  });

  it('editar la tarea de una modificación abre su formulario de edición', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    component.editTask(request(4, false, 'MODIFICATION'));
    expect(navigate).toHaveBeenCalledWith(['/tasks', 7, 'edit']);
  });

  it('descartar el aviso de una tarea vencida rechaza la solicitud sin tocar la tarea', () => {
    const req = request(5, true, 'EXPIRED');
    component.dismissExpired(req);
    expect(rejectRequest).toHaveBeenCalledWith(req);
    expect(resumeExpiredTask).not.toHaveBeenCalled();
  });

  it('reanudar una tarea vencida exige una fecha futura y aprueba la solicitud al terminar', () => {
    const req = request(6, true, 'EXPIRED');
    component.startResume(req);
    expect(component.resumingRequestId()).toBe(6);

    component.resumeDate.set('');
    component.confirmResume(req);
    component.resumeDate.set('2020-01-01T10:00');
    component.confirmResume(req);
    expect(resumeExpiredTask).not.toHaveBeenCalled();

    const future = new Date(Date.now() + 2 * 86_400_000);
    const local = new Date(future.getTime() - future.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
    component.resumeDate.set(local);
    component.confirmResume(req);

    expect(resumeExpiredTask).toHaveBeenCalledWith(req.task, expect.any(Date), expect.any(Function));
    resumeExpiredTask.mock.calls[0][2]();
    expect(approveRequest).toHaveBeenCalledWith(req);
    expect(component.resumingRequestId()).toBeNull();
  });

  it('approve delega al store', () => {
    const req = request(1);
    component.approve(req);
    expect(approveRequest).toHaveBeenCalledWith(req);
  });

  it('reject sin vencimiento delega al store directamente', () => {
    const req = request(1, false);
    component.reject(req);
    expect(rejectRequest).toHaveBeenCalledWith(req);
  });

  it('reject con vencimiento muestra confirmación; si se cancela no delega', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    component.reject(request(2, true));
    expect(rejectRequest).not.toHaveBeenCalled();
  });

  it('reject con vencimiento y confirmación sí delega', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const req = request(3, true);
    component.reject(req);
    expect(rejectRequest).toHaveBeenCalledWith(req);
  });

  it('isOverdue detecta plazos pasados y sin fecha', () => {
    expect(component.isOverdue(request(1, true))).toBe(true);
    expect(component.isOverdue(request(1, false))).toBe(false);
    const noDate = request(1);
    noDate.task.dueDate = undefined as never;
    expect(component.isOverdue(noDate)).toBe(false);
  });

  it('initialsOf devuelve las iniciales del asignado', () => {
    expect(component.initialsOf(request(1))).toBe('AT');
  });
});
