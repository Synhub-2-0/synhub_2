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
const request = (id: number, requestStatus: RequestStatus = 'PENDING', requestType: 'SUBMISSION' | 'MODIFICATION' | 'EXPIRED' = 'SUBMISSION') =>
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

  it('la bandeja pide por defecto las entregas pendientes y recuerda el estado elegido', () => {
    api.getRequestsByGroup.mockReturnValue(of([request(1), request(2)]));
    store.loadGroupRequests(10);
    expect(api.getRequestsByGroup).toHaveBeenCalledWith(10, 'PENDING', 'SUBMISSION');
    store.loadGroupRequests(10, 'APPROVED');
    expect(api.getRequestsByGroup).toHaveBeenLastCalledWith(10, 'APPROVED', 'SUBMISSION');
    expect(store.statusFilter()).toBe('APPROVED');
  });

  it('la bandeja se filtra por tipo: modificaciones y vencidas, y recuerda el tipo', () => {
    api.getRequestsByGroup.mockReturnValue(of([request(1, 'PENDING', 'MODIFICATION')]));
    store.loadGroupRequests(10, 'PENDING', 'MODIFICATION');
    expect(api.getRequestsByGroup).toHaveBeenLastCalledWith(10, 'PENDING', 'MODIFICATION');
    expect(store.typeFilter()).toBe('MODIFICATION');
    expect(store.groupRequests().map((r) => r.requestType)).toEqual(['MODIFICATION']);

    store.loadGroupRequests(10, 'APPROVED');
    expect(api.getRequestsByGroup).toHaveBeenLastCalledWith(10, 'APPROVED', 'MODIFICATION');
    store.loadGroupRequests(10, 'PENDING', 'EXPIRED');
    expect(api.getRequestsByGroup).toHaveBeenLastCalledWith(10, 'PENDING', 'EXPIRED');
  });

  it('si la bandeja falla queda vacía y el mensaje llega tal cual', () => {
    api.getRequestsByGroup.mockReturnValue(throwError(() => new Error('Solo el líder del grupo puede ver las validaciones.')));
    store.loadGroupRequests(10);
    expect(store.groupRequests()).toEqual([]);
    expect(toasts.toasts()[0].message).toBe('Solo el líder del grupo puede ver las validaciones.');
  });

  it('aprobar o rechazar saca la entrega de la bandeja de pendientes', () => {
    api.getRequestsByGroup.mockReturnValue(of([request(1), request(2)]));
    api.approveRequest.mockReturnValue(of(request(1, 'APPROVED')));
    api.rejectRequest.mockReturnValue(of(request(2, 'REJECTED')));
    store.loadGroupRequests(10);

    store.approveRequest(request(1));
    expect(api.approveRequest).toHaveBeenCalledWith(1);
    store.rejectRequest(request(2));

    expect(store.groupRequests()).toEqual([]);
    expect(store.processing()).toBe(false);
  });

  it('en otra pestaña la entrega revisada solo cambia de estado', () => {
    api.getRequestsByGroup.mockReturnValue(of([request(1, 'REJECTED')]));
    api.approveRequest.mockReturnValue(of(request(1, 'APPROVED')));
    store.loadGroupRequests(10, 'REJECTED');
    store.approveRequest(request(1, 'REJECTED'));
    expect(store.groupRequests().map((r) => r.requestStatus)).toEqual(['APPROVED']);
  });

  it('enviar una entrega la deja "en revisión" y llama a onSuccess', () => {
    api.createRequest.mockReturnValue(of(request(3)));
    const onSuccess = vi.fn();

    store.submitTask(7, onSuccess);

    const command = api.createRequest.mock.calls[0][0] as CreateRequestCommand;
    expect([command.taskId, command.description, command.requestType]).toEqual([7, 'Tarea completada.', 'SUBMISSION']);
    expect(store.isUnderReview(7)).toBe(true);
    expect(onSuccess).toHaveBeenCalledWith(expect.objectContaining({ id: 3 }));
  });

  it('isUnderReview ignora requests revisadas o de otro tipo', () => {
    api.getRequestsByTask.mockReturnValue(of([request(1, 'REJECTED'), request(2, 'PENDING', 'EXPIRED')]));
    store.loadTaskRequests(7);
    expect(store.isUnderReview(7)).toBe(false);
    expect(store.isUnderReview(99)).toBe(false);
  });

  it('pedir una modificación envía el comentario sin espacios y la deja pendiente', () => {
    api.createRequest.mockReturnValue(of(request(4, 'PENDING', 'MODIFICATION')));
    const onSuccess = vi.fn();

    store.createModification(7, '  Cambiar el alcance  ', onSuccess);

    const command = api.createRequest.mock.calls[0][0] as CreateRequestCommand;
    expect([command.taskId, command.description, command.requestType]).toEqual([7, 'Cambiar el alcance', 'MODIFICATION']);
    expect(store.hasPendingModification(7)).toBe(true);
    expect(store.isUnderReview(7)).toBe(false);
    expect(onSuccess).toHaveBeenCalledWith(expect.objectContaining({ id: 4 }));
  });

  it('hasPendingModification ignora las modificaciones resueltas y las entregas', () => {
    api.getRequestsByTask.mockReturnValue(of([request(1, 'APPROVED', 'MODIFICATION'), request(2, 'PENDING', 'SUBMISSION')]));
    store.loadTaskRequests(7);
    expect(store.hasPendingModification(7)).toBe(false);
    expect(store.hasPendingModification(99)).toBe(false);
  });

  it('un error al pedir la modificación se muestra y no deja nada pendiente', () => {
    api.createRequest.mockReturnValue(throwError(() => new Error('La tarea debe estar en progreso para pedir una modificación.')));
    store.createModification(7, 'x');
    expect(store.error()).toBe('La tarea debe estar en progreso para pedir una modificación.');
    expect(store.hasPendingModification(7)).toBe(false);
  });

  it('un error al enviar se muestra y no marca la tarea', () => {
    api.createRequest.mockReturnValue(throwError(() => new Error('Esta tarea ya tiene una entrega en revisión.')));
    store.submitTask(7);
    expect(store.error()).toBe('Esta tarea ya tiene una entrega en revisión.');
    expect(store.isUnderReview(7)).toBe(false);
  });

  it('al cerrar sesión se vacía todo', () => {
    api.getRequestsByGroup.mockReturnValue(of([request(1)]));
    store.loadGroupRequests(10, 'APPROVED', 'EXPIRED');
    isSignedIn.set(false);
    TestBed.tick();
    expect(store.groupRequests()).toEqual([]);
    expect(store.statusFilter()).toBe('PENDING');
    expect(store.typeFilter()).toBe('SUBMISSION');
  });
});
