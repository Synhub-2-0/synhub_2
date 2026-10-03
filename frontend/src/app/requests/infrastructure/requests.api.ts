import {Injectable} from '@angular/core';
import {BaseApi} from '../../shared/infrastructure/base-api';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {RequestsApiEndpoint} from './requests.api-endpoint';
import {RequestStatus, RequestType, TaskRequest} from '../domain/model/task-request.entity';
import {CreateRequestCommand} from '../domain/model/create-request.command';

@Injectable({providedIn: 'root'})
export class RequestsApi extends BaseApi {
  private readonly requestsEndpoint: RequestsApiEndpoint;

  constructor(http: HttpClient) {
    super();
    this.requestsEndpoint = new RequestsApiEndpoint(http);
  }

  createRequest(command: CreateRequestCommand): Observable<TaskRequest> {
    return this.requestsEndpoint.createRequest(command);
  }

  getRequestsByTask(taskId: number): Observable<TaskRequest[]> {
    return this.requestsEndpoint.getRequestsByTask(taskId);
  }

  getRequestsByGroup(groupId: number, status?: RequestStatus, type?: RequestType): Observable<TaskRequest[]> {
    return this.requestsEndpoint.getRequestsByGroup(groupId, status, type);
  }

  approveRequest(taskId: number, requestId: number): Observable<TaskRequest> {
    return this.requestsEndpoint.reviewRequest(taskId, requestId, 'APPROVED');
  }

  rejectRequest(taskId: number, requestId: number): Observable<TaskRequest> {
    return this.requestsEndpoint.reviewRequest(taskId, requestId, 'REJECTED');
  }
}
