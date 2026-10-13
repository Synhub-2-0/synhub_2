import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RequestsApi } from './requests.api';
import { CreateRequestCommand } from '../domain/model/create-request.command';
import { environment } from '../../../environments/environment';

describe('RequestsApi (HTTP)', () => {
  const base = environment.platformProviderApiBaseUrl;
  const tasks = `${base}${environment.platformProviderTasksEndpointPath}`;
  const groups = `${base}${environment.platformProviderGroupsEndpointPath}`;
  const resource = {
    id: 3, description: 'Listo', requestType: 'SUBMISSION', requestStatus: 'PENDING',
    task: { id: 7, title: 'T', description: '', dueDate: '2026-12-01T10:00:00Z', createdAt: '2026-10-01T10:00:00Z', updatedAt: '2026-10-01T10:00:00Z', status: 'COMPLETED' },
  };
  let api: RequestsApi;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(RequestsApi);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('rutas anidadas bajo tasks y groups', () => {
    api.createRequest(new CreateRequestCommand({ taskId: 7, description: 'Listo', requestType: 'SUBMISSION' })).subscribe();
    api.getRequestsByTask(7).subscribe();
    api.getRequestsByGroup(10, 'PENDING', 'SUBMISSION').subscribe();
    api.approveRequest(7, 3).subscribe();
    api.rejectRequest(7, 3).subscribe();

    const post = backend.expectOne({ method: 'POST', url: `${tasks}/7/requests` });
    expect(post.request.body).toEqual({ description: 'Listo', requestType: 'SUBMISSION' });
    post.flush(resource);
    backend.expectOne({ method: 'GET', url: `${tasks}/7/requests` }).flush([resource]);
    backend.expectOne({ method: 'GET', url: `${groups}/10/requests?status=PENDING&type=SUBMISSION` }).flush({ requests: [resource] });
    backend.expectOne({ method: 'PUT', url: `${tasks}/7/requests/3/status/APPROVED` }).flush(resource);
    backend.expectOne({ method: 'PUT', url: `${tasks}/7/requests/3/status/REJECTED` }).flush(resource);
  });

  it.each([
    [403, '', 'Solo el integrante asignado a la tarea puede enviar la entrega.'],
    [404, '', 'La tarea no existe.'],
    [409, 'Task 7 already has a pending submission', 'Esta tarea ya tiene una entrega en revisión.'],
    [409, 'A submission can only be created for a COMPLETED task', 'La tarea debe estar completada para poder enviar la entrega.'],
  ])('al entregar, un %i se traduce a un mensaje en español', (status, backendMessage, expected) => {
    let message = '';
    api.createRequest(new CreateRequestCommand({ taskId: 7, description: 'x', requestType: 'SUBMISSION' }))
      .subscribe({ error: (e: Error) => (message = e.message) });
    backend.expectOne(`${tasks}/7/requests`).flush({ message: backendMessage }, { status, statusText: 'Error' });
    expect(message).toBe(expected);
  });

  it.each([
    [403, 'Solo el líder del grupo puede ver las validaciones.'],
    [404, 'El grupo no existe.'],
    [500, 'No se pudieron cargar las entregas.'],
  ])('al listar, un %i se traduce a un mensaje en español', (status, expected) => {
    let message = '';
    api.getRequestsByGroup(10).subscribe({ error: (e: Error) => (message = e.message) });
    backend.expectOne(`${groups}/10/requests`).flush({}, { status, statusText: 'Error' });
    expect(message).toBe(expected);
  });

  it.each([
    [403, '', 'Solo el líder del grupo puede revisar entregas.'],
    [404, '', 'La entrega ya no existe.'],
    [409, 'Request 3 was already reviewed', 'Esta entrega ya fue revisada.'],
    [409, 'Only a COMPLETED task can be approved', 'La tarea ya no está completada, por lo que no se puede aprobar. Recarga la lista.'],
  ])('al revisar, un %i se traduce a un mensaje en español', (status, backendMessage, expected) => {
    let message = '';
    api.approveRequest(7, 3).subscribe({ error: (e: Error) => (message = e.message) });
    backend.expectOne(`${tasks}/7/requests/3/status/APPROVED`).flush({ message: backendMessage }, { status, statusText: 'Error' });
    expect(message).toBe(expected);
  });

});
