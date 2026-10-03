import {environment} from '../../../environments/environment';
import {BaseApiEndpoint} from '../../shared/infrastructure/base-api-endpoint';
import {RequestStatus, RequestType, TaskRequest} from '../domain/model/task-request.entity';
import {CreateRequestCommand} from '../domain/model/create-request.command';
import {RequestResource, RequestsResponse} from './requests.response';
import {RequestsAssembler} from './requests.assembler';
import {HttpClient, HttpErrorResponse, HttpParams} from '@angular/common/http';
import {catchError, map, Observable, throwError} from 'rxjs';

const tasksEndpointUrl = `${environment.platformProviderApiBaseUrl}${environment.platformProviderTasksEndpointPath}`;
const groupsEndpointUrl = `${environment.platformProviderApiBaseUrl}${environment.platformProviderGroupsEndpointPath}`;
const requestsPath = environment.platformProviderRequestsEndpointPath;

export class RequestsApiEndpoint extends BaseApiEndpoint<TaskRequest, RequestResource, RequestsResponse, RequestsAssembler> {
  constructor(http: HttpClient) {
    super(http, tasksEndpointUrl, new RequestsAssembler());
  }

  /** POST /api/v1/tasks/{taskId}/requests */
  createRequest(command: CreateRequestCommand): Observable<TaskRequest> {
    const body = this.assembler.toRequestFromCreateCommand(command);
    return this.http.post<RequestResource>(`${tasksEndpointUrl}/${command.taskId}${requestsPath}`, body).pipe(
      map(resource => this.assembler.toEntityFromResource(resource)),
      catchError((error: HttpErrorResponse) => throwError(() => new Error(this.createErrorMessage(error))))
    );
  }

  /** GET /api/v1/tasks/{taskId}/requests */
  getRequestsByTask(taskId: number): Observable<TaskRequest[]> {
    return this.http.get<RequestsResponse | RequestResource[]>(`${tasksEndpointUrl}/${taskId}${requestsPath}`).pipe(
      map(response => this.toEntities(response)),
      catchError(this.handleError(`Failed to fetch requests for task with ID ${taskId}`))
    );
  }

  /** GET /api/v1/groups/{groupId}/requests?status=...&type=... (leader only) */
  getRequestsByGroup(groupId: number, status?: RequestStatus, type?: RequestType): Observable<TaskRequest[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    if (type) params = params.set('type', type);
    return this.http.get<RequestsResponse | RequestResource[]>(`${groupsEndpointUrl}/${groupId}${requestsPath}`, {params}).pipe(
      map(response => this.toEntities(response)),
      catchError((error: HttpErrorResponse) => throwError(() => new Error(this.listErrorMessage(error))))
    );
  }

  /** PUT /api/v1/tasks/{taskId}/requests/{requestId}/status/{status} (leader only) */
  reviewRequest(taskId: number, requestId: number, status: Exclude<RequestStatus, 'PENDING'>): Observable<TaskRequest> {
    return this.http.put<RequestResource>(`${tasksEndpointUrl}/${taskId}${requestsPath}/${requestId}/status/${status}`, {}).pipe(
      map(resource => this.assembler.toEntityFromResource(resource)),
      catchError((error: HttpErrorResponse) => throwError(() => new Error(this.reviewErrorMessage(error))))
    );
  }

  private toEntities(response: RequestsResponse | RequestResource[]): TaskRequest[] {
    return Array.isArray(response)
      ? response.map(resource => this.assembler.toEntityFromResource(resource))
      : this.assembler.toEntitiesFromResponse(response);
  }

  // The backend answers in English with ids, so the status (and the 409 message) is mapped to a user-facing text.
  private createErrorMessage(error: HttpErrorResponse): string {
    const backendMessage: string = error.error?.message ?? '';
    switch (error.status) {
      case 403: return 'Solo el integrante asignado a la tarea puede enviar la entrega.';
      case 404: return 'La tarea no existe.';
      case 409: return backendMessage.includes('pending submission')
        ? 'Esta tarea ya tiene una entrega en revisión.'
        : 'La tarea debe estar completada para poder enviar la entrega.';
      default: return 'No se pudo enviar la entrega.';
    }
  }

  private listErrorMessage(error: HttpErrorResponse): string {
    switch (error.status) {
      case 403: return 'Solo el líder del grupo puede ver las validaciones.';
      case 404: return 'El grupo no existe.';
      default: return 'No se pudieron cargar las entregas.';
    }
  }

  private reviewErrorMessage(error: HttpErrorResponse): string {
    const backendMessage: string = error.error?.message ?? '';
    switch (error.status) {
      case 403: return 'Solo el líder del grupo puede revisar entregas.';
      case 404: return 'La entrega ya no existe.';
      case 409: return backendMessage.includes('already reviewed')
        ? 'Esta entrega ya fue revisada.'
        : 'La tarea ya no está completada, por lo que no se puede aprobar. Recarga la lista.';
      default: return 'No se pudo revisar la entrega.';
    }
  }
}
