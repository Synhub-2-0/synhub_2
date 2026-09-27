import { environment } from '../../../environments/environment';
import { BaseApiEndpoint } from '../../shared/infrastructure/base-api-endpoint';
import { Task, TaskStatus } from '../domain/model/task.entity';
import { CreateTaskCommand } from '../domain/model/create-task.command';
import { UpdateTaskCommand } from '../domain/model/update-task.command';
import { TaskResource, TasksResponse } from './tasks.response';
import { TasksAssembler } from './tasks.assembler';
import { HttpClient, HttpParams } from '@angular/common/http';
import { catchError, map, Observable } from 'rxjs';

const tasksEndpointUrl = `${environment.platformProviderApiBaseUrl}${environment.platformProviderTasksEndpointPath}`;

export class TasksApiEndpoint extends BaseApiEndpoint<
  Task,
  TaskResource,
  TasksResponse,
  TasksAssembler
> {
  constructor(http: HttpClient) {
    super(http, tasksEndpointUrl, new TasksAssembler());
  }

  /** POST /api/v1/tasks */
  createTask(command: CreateTaskCommand): Observable<Task> {
    const body = this.assembler.toCreateRequestFromCommand(command);
    return this.http.post<TaskResource>(tasksEndpointUrl, body).pipe(
      map((resource) => this.assembler.toEntityFromResource(resource)),
      catchError(this.handleError('Failed to create task')),
    );
  }

  /** PUT /api/v1/tasks/{taskId} */
  updateTaskDetails(taskId: number, command: UpdateTaskCommand): Observable<Task> {
    const body = this.assembler.toUpdateRequestFromCommand(command);
    return this.http.put<TaskResource>(`${tasksEndpointUrl}/${taskId}`, body).pipe(
      map((resource) => this.assembler.toEntityFromResource(resource)),
      catchError(this.handleError(`Failed to update task with ID ${taskId}`)),
    );
  }

  /** PUT /api/v1/tasks/{taskId}/status?status=... */
  updateTaskStatus(taskId: number, status: TaskStatus): Observable<Task> {
    const params = new HttpParams().set('status', status);
    return this.http.put<TaskResource>(`${tasksEndpointUrl}/${taskId}/status`, {}, { params }).pipe(
      map((resource) => this.assembler.toEntityFromResource(resource)),
      catchError(this.handleError(`Failed to update status for task with ID ${taskId}`)),
    );
  }

  /** GET /api/v1/tasks/group/{groupId}?status=... */
  getTasksByGroup(groupId: number, status?: TaskStatus): Observable<Task[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }

    return this.http
      .get<TasksResponse | TaskResource[]>(`${tasksEndpointUrl}/group/${groupId}`, { params })
      .pipe(
        map((response) => {
          if (Array.isArray(response)) {
            return response.map((resource) => this.assembler.toEntityFromResource(resource));
          }
          return this.assembler.toEntitiesFromResponse(response as TasksResponse);
        }),
        catchError(this.handleError(`Failed to fetch tasks for group ${groupId}`)),
      );
  }

  /** GET /api/v1/tasks/group/{groupId}/user/{userId} */
  getTasksByGroupAndUser(groupId: number, userId: number): Observable<Task[]> {
    return this.http
      .get<TasksResponse | TaskResource[]>(`${tasksEndpointUrl}/group/${groupId}/user/${userId}`)
      .pipe(
        map((response) => {
          if (Array.isArray(response)) {
            return response.map((resource) => this.assembler.toEntityFromResource(resource));
          }
          return this.assembler.toEntitiesFromResponse(response as TasksResponse);
        }),
        catchError(
          this.handleError(`Failed to fetch tasks for group ${groupId} and user ${userId}`),
        ),
      );
  }
}
