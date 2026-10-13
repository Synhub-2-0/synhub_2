import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TasksApi } from './tasks.api';
import { CreateTaskCommand } from '../domain/model/create-task.command';
import { UpdateTaskCommand } from '../domain/model/update-task.command';
import { Task } from '../domain/model/task.entity';
import { environment } from '../../../environments/environment';

describe('TasksApi (HTTP)', () => {
  const url = `${environment.platformProviderApiBaseUrl}${environment.platformProviderTasksEndpointPath}`;
  const resource = {
    id: 7, title: 'T', description: 'd', dueDate: '2026-12-01T10:00:00Z', createdAt: '2026-10-01T10:00:00Z',
    updatedAt: '2026-10-01T10:00:00Z', status: 'IN_PROGRESS',
  };
  let api: TasksApi;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(TasksApi);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('tareas por grupo con y sin estado, y por usuario', () => {
    let tasks: Task[] = [];
    api.getTasksByGroup(10).subscribe((t) => (tasks = t));
    api.getTasksByGroup(10, 'DONE').subscribe();
    api.getTasksByGroupAndUser(10, 5).subscribe();
    backend.expectOne(`${url}/group/10`).flush([resource]);
    backend.expectOne(`${url}/group/10?status=DONE`).flush({ tasks: [] });
    backend.expectOne(`${url}/group/10/user/5`).flush([]);
    expect(tasks[0].dueDate).toEqual(new Date('2026-12-01T10:00:00Z'));
  });

  it('el estado viaja como query param y sin cuerpo', () => {
    api.updateTaskStatus(7, 'COMPLETED').subscribe();
    const req = backend.expectOne(`${url}/7/status?status=COMPLETED`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({});
    req.flush(resource);
  });

  it('crear, leer, editar y borrar', () => {
    const due = new Date('2026-12-01T10:00:00Z');
    api.createTask(new CreateTaskCommand('T', 'd', due, 5, 10, 3, 'QA', 'LOW', 'x', true)).subscribe();
    api.getTaskById(7).subscribe();
    api.updateTask(7, new UpdateTaskCommand('T2', 'd', due, 5)).subscribe();
    api.deleteTask(7).subscribe();

    const post = backend.expectOne({ method: 'POST', url });
    expect(post.request.body).toMatchObject({ title: 'T', dueDate: '2026-12-01T10:00:00.000Z', userId: 5, groupId: 10, difficulty: 3 });
    post.flush(resource);
    backend.expectOne({ method: 'GET', url: `${url}/7` }).flush(resource);
    const put = backend.expectOne({ method: 'PUT', url: `${url}/7` });
    expect(put.request.body).toMatchObject({ title: 'T2', userId: 5 });
    expect(put.request.body).not.toHaveProperty('requesterId'); // AUDIT BUG-16
    put.flush(resource);
    backend.expectOne({ method: 'DELETE', url: `${url}/7` }).flush(null);
  });
});
