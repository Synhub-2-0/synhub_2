import { ComponentFixture, TestBed } from '@angular/core/testing';
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
  let fixture: ComponentFixture<Validations>;
  let groupRequests: ReturnType<typeof signal<TaskRequest[]>>;
  let leaderGroups: ReturnType<typeof signal<never[]>>;

  beforeEach(async () => {
    localStorage.clear();
    groupRequests = signal<TaskRequest[]>([]);
    leaderGroups = signal<never[]>([]);
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
            groupRequests, loading: signal(false), processing: signal(false),
            statusFilter: signal('PENDING'), typeFilter: signal('SUBMISSION'),
          },
        },
        { provide: TasksStore, useValue: { resumeExpiredTask } },
        {
          provide: GroupsStore,
          useValue: {
            loadGroups: vi.fn(), leaderGroups, memberGroups: signal([]),
            groups: signal([]), loading: signal(false),
          },
        },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({}) } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Validations);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('la tarjeta de una tarea vencida solo ofrece "Reanudar tarea"', async () => {
    groupRequests.set([request(9, true, 'EXPIRED')]);
    leaderGroups.set([{ id: 10, name: 'G', imgUrl: '' }] as never[]);
    fixture.detectChanges();
    await fixture.whenStable();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Reanudar tarea');
    expect(text).not.toContain('Descartar');
    expect(text).not.toContain('Rechazar');
  });

  it('se crea y ofrece los filtros de estado y de tipo', () => {
    expect(component).toBeTruthy();
    expect(component.statusOptions.map((o) => o.value)).toEqual(['PENDING', 'APPROVED', 'REJECTED']);
    expect(component.typeOptions.map((o) => o.value)).toEqual(['ALL', 'SUBMISSION', 'MODIFICATION', 'EXPIRED']);
    expect(component.typeOptions[0].label).toBe('Todas');
  });

  it('las solicitudes de un grupo bloqueado no se pueden atender', () => {
    const open = request(1);
    const locked = request(2);
    (locked.task as unknown as { _group: unknown })._group = { id: 3, locked: true };
    expect(component.isLocked(open)).toBe(false);
    expect(component.isLocked(locked)).toBe(true);
  });

  it('elegir "Todas" recarga la bandeja sin filtrar por tipo', () => {
    component.selectedGroupId.set(10);
    component.setType('ALL');
    expect(loadGroupRequests).toHaveBeenLastCalledWith(10, undefined, 'ALL');
  });

  it('el líder puede denegar una solicitud de modificación sin confirmar si el plazo sigue abierto', () => {
    const req = request(7, false, 'MODIFICATION');
    component.reject(req);
    expect(rejectRequest).toHaveBeenCalledWith(req);
  });

  it('denegar una modificación con el plazo vencido avisa que la tarea pasará a Vencida', () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);
    const req = request(8, true, 'MODIFICATION');

    component.reject(req);

    expect(confirmSpy.mock.calls[0][0]).toContain('solicitud');
    expect(confirmSpy.mock.calls[0][0]).toContain('En progreso');
    expect(rejectRequest).not.toHaveBeenCalled();
  });

  it('cada tipo tiene su color: entregas verde, modificaciones amarillo y vencidas rojo', () => {
    expect(component.typeChipClass.ALL.on).toContain('slate');
    expect(component.typeChipClass.SUBMISSION.on).toContain('emerald');
    expect(component.typeChipClass.MODIFICATION.on).toContain('amber');
    expect(component.typeChipClass.EXPIRED.on).toContain('red');
    expect(component.typeBadgeClass.SUBMISSION).toContain('emerald');
    expect(component.typeBadgeClass.MODIFICATION).toContain('amber');
    expect(component.typeBadgeClass.EXPIRED).toContain('red');
  });

  it('cambiar el tipo recarga la bandeja del grupo elegido con ese tipo', () => {
    component.selectedGroupId.set(10);
    component.setType('MODIFICATION');
    expect(loadGroupRequests).toHaveBeenLastCalledWith(10, undefined, 'MODIFICATION');
  });

  it('editar la tarea de una modificación abre su formulario de edición', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    component.selectedGroupId.set(10);
    component.editTask(request(4, false, 'MODIFICATION'));
    expect(navigate).toHaveBeenCalledWith(['/tasks', 7, 'edit'], { queryParams: { returnTo: '/validations?groupId=10' } });
  });

  it('una solicitud de vencimiento solo se puede reanudar: sin descartar ni rechazar', () => {
    expect('dismissExpired' in component).toBe(false);
    const req = request(5, true, 'EXPIRED');
    component.startResume(req);
    expect(component.resumingRequestId()).toBe(5);
    expect(rejectRequest).not.toHaveBeenCalled();
  });

  it('reanudar una tarea vencida exige una fecha futura y recarga la bandeja al terminar', () => {
    component.selectedGroupId.set(10);
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
    expect(approveRequest).not.toHaveBeenCalled(); // el backend cierra el aviso al reanudar
    expect(loadGroupRequests).toHaveBeenLastCalledWith(10);
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
