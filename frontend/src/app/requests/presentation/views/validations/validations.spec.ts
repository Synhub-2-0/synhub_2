import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Validations } from './validations';
import { RequestsApi } from '../../../infrastructure/requests.api';
import { RequestsStore } from '../../../application/requests.store';
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

const request = (id: number, overdue = false) => new TaskRequest({
  id, description: 'Listo', requestType: 'SUBMISSION', requestStatus: 'PENDING', task: task(overdue),
});

describe('Validations', () => {
  let approveSubmission: ReturnType<typeof vi.fn>;
  let rejectSubmission: ReturnType<typeof vi.fn>;
  let loadGroupSubmissions: ReturnType<typeof vi.fn>;
  let component: Validations;

  beforeEach(async () => {
    localStorage.clear();
    approveSubmission = vi.fn();
    rejectSubmission = vi.fn();
    loadGroupSubmissions = vi.fn();
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
            approveSubmission, rejectSubmission, loadGroupSubmissions,
            submissions: signal([]), processing: signal(false),
            statusFilter: signal('PENDING'), setStatusFilter: vi.fn(),
          },
        },
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

  it('se crea y ofrece los filtros de estado', () => {
    expect(component).toBeTruthy();
    expect(component.statusOptions.map((o) => o.value)).toEqual(['PENDING', 'APPROVED', 'REJECTED']);
  });

  it('approve delega al store', () => {
    const req = request(1);
    component.approve(req);
    expect(approveSubmission).toHaveBeenCalledWith(req);
  });

  it('reject sin vencimiento delega al store directamente', () => {
    const req = request(1, false);
    component.reject(req);
    expect(rejectSubmission).toHaveBeenCalledWith(req);
  });

  it('reject con vencimiento muestra confirmación; si se cancela no delega', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    component.reject(request(2, true));
    expect(rejectSubmission).not.toHaveBeenCalled();
  });

  it('reject con vencimiento y confirmación sí delega', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const req = request(3, true);
    component.reject(req);
    expect(rejectSubmission).toHaveBeenCalledWith(req);
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
