import {environment} from '../../../environments/environment';
import {BaseApiEndpoint} from '../../shared/infrastructure/base-api-endpoint';
import {RequestStatus, RequestType, TaskRequest} from '../domain/model/task-request.entity';
import {CreateRequestCommand} from '../domain/model/create-request.command';
import {RequestResource, RequestsResponse} from './requests.response';
import {RequestsAssembler} from './requests.assembler';
import {HttpClient, HttpErrorResponse, HttpParams} from '@angular/common/http';
import {catchError, map, Observable, throwError} from 'rxjs';

const requestsEndpointUrl = `${environment.platformProviderApiBaseUrl}${environment.platformProviderRequestsEndpointPath}`;

export class RequestsApiEndpoint extends BaseApiEndpoint<TaskRequest, RequestResource, RequestsResponse, RequestsAssembler> {
  constructor(http: HttpClient) {
    super(http, requestsEndpointUrl, new RequestsAssembler());
  }

  /** POST /api/v1/requests (the task travels in the body) */
  createRequest(command: CreateRequestCommand): Observable<TaskRequest> {
    const body = this.assembler.toRequestFromCreateCommand(command);
    return this.http.post<RequestResource>(requestsEndpointUrl, body).pipe(
      map(resource => this.assembler.toEntityFromResource(resource)),
      catchError((error: HttpErrorResponse) => throwError(() => new Error(this.createErrorMessage(error, command.requestType))))
    );
  }

  /** GET /api/v1/requests/task?taskId={taskId} */
  getRequestsByTask(taskId: number): Observable<TaskRequest[]> {
    const params = new HttpParams().set('taskId', taskId);
    return this.http.get<RequestsResponse | RequestResource[]>(`${requestsEndpointUrl}/task`, {params}).pipe(
      map(response => this.toEntities(response)),
      catchError(this.handleError(`Failed to fetch requests for task with ID ${taskId}`))
    );
  }

  /** GET /api/v1/requests/group?groupId=...&status=...&type=... (leader only) */
  getRequestsByGroup(groupId: number, status?: RequestStatus, type?: RequestType): Observable<TaskRequest[]> {
    let params = new HttpParams().set('groupId', groupId);
    if (status) params = params.set('status', status);
    if (type) params = params.set('type', type);
    return this.http.get<RequestsResponse | RequestResource[]>(`${requestsEndpointUrl}/group`, {params}).pipe(
      map(response => this.toEntities(response)),
      catchError((error: HttpErrorResponse) => throwError(() => new Error(this.listErrorMessage(error))))
    );
  }

  /** PUT /api/v1/requests/{requestId}/status/{status} (leader only) */
  reviewRequest(requestId: number, status: Exclude<RequestStatus, 'PENDING'>): Observable<TaskRequest> {
    return this.http.put<RequestResource>(`${requestsEndpointUrl}/${requestId}/status/${status}`, {}).pipe(
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
  private createErrorMessage(error: HttpErrorResponse, type: RequestType): string {
    const backendMessage: string = error.error?.message ?? '';
    if (type === 'MODIFICATION') {
      switch (error.status) {
        case 400: return 'Escribe qué quieres que se modifique de la tarea.';
        case 403: return 'Solo el integrante asignado a la tarea puede pedir una modificación.';
        case 404: return 'La tarea no existe.';
        case 409: return backendMessage.includes('pending request')
          ? 'Esta tarea ya tiene una solicitud pendiente.'
          : 'La tarea debe estar en progreso para pedir una modificación.';
        default: return 'No se pudo enviar la solicitud de modificación.';
      }
    }
    switch (error.status) {
      case 403: return 'Solo el integrante asignado a la tarea puede enviar la entrega.';
      case 404: return 'La tarea no existe.';
      case 409: return backendMessage.includes('pending request')
        ? 'Esta tarea ya tiene una entrega en revisión.'
        : 'La tarea debe estar en progreso o completada para poder enviar la entrega.';
      default: return 'No se pudo enviar la entrega.';
    }
  }

  private listErrorMessage(error: HttpErrorResponse): string {
    switch (error.status) {
      case 403: return 'Solo el líder del grupo puede ver las validaciones.';
      case 404: return 'El grupo no existe.';
      default: return 'No se pudieron cargar las solicitudes.';
    }
  }

  private reviewErrorMessage(error: HttpErrorResponse): string {
    const backendMessage: string = error.error?.message ?? '';
    switch (error.status) {
      case 403: return 'Solo el líder del grupo puede revisar entregas.';
      case 404: return 'La entrega ya no existe.';
      case 409:
        if (backendMessage.includes('resolved by editing')) return 'Las modificaciones se resuelven editando la tarea.';
        return backendMessage.includes('already reviewed')
          ? 'Esta entrega ya fue revisada.'
          : 'La tarea ya no está completada, por lo que no se puede aprobar. Recarga la lista.';
      default: return 'No se pudo revisar la entrega.';
    }
  }
}
