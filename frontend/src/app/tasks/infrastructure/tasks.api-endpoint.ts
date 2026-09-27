import {environment} from '../../../environments/environment';
import {BaseApiEndpoint} from '../../shared/infrastructure/base-api-endpoint';
import {Task} from '../domain/model/task.entity';
import {TaskResource, TasksResponse} from './tasks.response';
import {TasksAssembler} from './tasks.assembler';
import {HttpClient} from '@angular/common/http';
import {catchError, map, Observable} from 'rxjs';

const tasksEndpointUrl = `${environment.platformProviderApiBaseUrl}${environment.platformProviderTasksEndpointPath}`

export class TasksApiEndpoint extends BaseApiEndpoint<Task, TaskResource, TasksResponse, TasksAssembler> {
  constructor(http: HttpClient) {
    super(http, tasksEndpointUrl, new TasksAssembler());
  }

  updateTaskStatus(taskId: number, status: string): Observable<Task> {
    return this.http.put<Task>(`${tasksEndpointUrl}/${taskId}/status/${status}`, {}).pipe(
      catchError(this.handleError(`Failed to update status for task with ID ${taskId}`))
    )
  }

  getTasksByStatus(status: string): Observable<Task[]> {
    return this.http.get<TasksResponse | TaskResource[]>(`${tasksEndpointUrl}/status/${status}`).pipe(
      map(response => {
        if (Array.isArray(response))
          return response.map(resource => this.assembler.toEntityFromResource(resource));
        return this.assembler.toEntitiesFromResponse(response as TasksResponse);
      }),
      catchError(this.handleError(`Failed to fetch tasks with status ${status}`))
    );
  }
}
