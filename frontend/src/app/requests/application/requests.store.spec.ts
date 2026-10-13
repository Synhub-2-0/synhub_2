import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { RequestsStore } from './requests.store';
import { RequestsApi } from '../infrastructure/requests.api';
import { IamStore } from '../../iam/application/iam.store';
import { ToastStore } from '../../shared/application/toast.store';
import { RequestStatus, TaskRequest } from '../domain/model/task-request.entity';
import { CreateRequestCommand } from '../domain/model/create-request.command';
import { Task } from '../../tasks/domain/model/task.entity';

const task = new Task({ id: 7, title: 'T', description: '', dueDate: new Date(), createdAt: new Date(), updatedAt: new Date(), status: 'COMPLETED' });
const request = (id: number, requestStatus: RequestStatus = 'PENDING', requestType: 'SUBMISSION' | 'EXPIRED' = 'SUBMISSION') =>
  new TaskRequest({ id, description: 'Listo', requestType, requestStatus, task });

describe('RequestsStore', () => {
  let api: Record<'getRequestsByGroup' | 'approveRequest' | 'rejectRequest' | 'createRequest' | 'getRequestsByTask', ReturnType<typeof vi.fn>>;
  let isSignedIn: ReturnType<typeof signal<boolean>>;
  let store: RequestsStore;
  let toasts: ToastStore;

  beforeEach(() => {
    api = { getRequestsByGroup: vi.fn(), approveRequest: vi.fn(), rejectRequest: vi.fn(), createRequest: vi.fn(), getRequestsByTask: vi.fn() };
    isSignedIn = signal(true);
    TestBed.configureTestingModule({
      providers: [{ provide: RequestsApi, useValue: api }, { provide: IamStore, useValue: { isSignedIn } }],
    });
    store = TestBed.inject(RequestsStore);
    toasts = TestBed.inject(ToastStore);
  });

  it('la bandeja pide solo entregas con el estado elegido', () => {
    api.getRequestsByGroup.mockReturnValue(of([request(1), request(2)]));
    store.loadGroupSubmissions(10);
    expect(api.getRequestsByGroup).toHaveBeenCalledWith(10, 'PENDING', 'SUBMISSION');
    store.loadGroupSubmissions(10, 'APPROVED');
    expect(api.getRequestsByGroup).toHaveBeenLastCalledWith(10, 'APPROVED', 'SUBMISSION');
    expect(store.statusFilter()).toBe('APPROVED');
  });

  it('si la bandeja falla queda vacía y el mensaje llega tal cual', () => {
    api.getRequestsByGroup.mockReturnValue(throwError(() => new Error('Solo el líder del grupo puede ver las validaciones.')));
    store.loadGroupSubmissions(10);
    expect(store.submissions()).toEqual([]);
    expect(toasts.toasts()[0].message).toBe('Solo el líder del grupo puede ver las validaciones.');
  });

  it('aprobar o rechazar saca la entrega de la bandeja de pendientes', () => {
    api.getRequestsByGroup.mockReturnValue(of([request(1), request(2)]));
    api.approveRequest.mockReturnValue(of(request(1, 'APPROVED')));
    api.rejectRequest.mockReturnValue(of(request(2, 'REJECTED')));
    store.loadGroupSubmissions(10);

    store.approveSubmission(request(1));
    expect(api.approveRequest).toHaveBeenCalledWith(7, 1);
    store.rejectSubmission(request(2));

    expect(store.submissions()).toEqual([]);
    expect(store.processing()).toBe(false);
  });

  it('en otra pestaña la entrega revisada solo cambia de estado', () => {
    api.getRequestsByGroup.mockReturnValue(of([request(1, 'REJECTED')]));
    api.approveRequest.mockReturnValue(of(request(1, 'APPROVED')));
    store.loadGroupSubmissions(10, 'REJECTED');
    store.approveSubmission(request(1, 'REJECTED'));
    expect(store.submissions().map((r) => r.requestStatus)).toEqual(['APPROVED']);
  });

  it('enviar una entrega la deja "en revisión" y llama a onSuccess', () => {
    api.createRequest.mockReturnValue(of(request(3)));
    const onSuccess = vi.fn();

    store.submitTask(7, 'Listo', onSuccess);

    const command = api.createRequest.mock.calls[0][0] as CreateRequestCommand;
    expect([command.taskId, command.description, command.requestType]).toEqual([7, 'Listo', 'SUBMISSION']);
    expect(store.isUnderReview(7)).toBe(true);
    expect(onSuccess).toHaveBeenCalled();
  });

  it('isUnderReview ignora requests revisadas o de otro tipo', () => {
    api.getRequestsByTask.mockReturnValue(of([request(1, 'REJECTED'), request(2, 'PENDING', 'EXPIRED')]));
    store.loadTaskRequests(7);
    expect(store.isUnderReview(7)).toBe(false);
    expect(store.isUnderReview(99)).toBe(false);
  });

  it('un error al enviar se muestra y no marca la tarea', () => {
    api.createRequest.mockReturnValue(throwError(() => new Error('Esta tarea ya tiene una entrega en revisión.')));
    store.submitTask(7, 'x');
    expect(store.error()).toBe('Esta tarea ya tiene una entrega en revisión.');
    expect(store.isUnderReview(7)).toBe(false);
  });

  it('al cerrar sesión se vacía todo', () => {
    api.getRequestsByGroup.mockReturnValue(of([request(1)]));
    store.loadGroupSubmissions(10, 'APPROVED');
    isSignedIn.set(false);
    TestBed.tick();
    expect(store.submissions()).toEqual([]);
    expect(store.statusFilter()).toBe('PENDING');
  });
});
