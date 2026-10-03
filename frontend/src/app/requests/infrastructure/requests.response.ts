import {BaseResource, BaseResponse} from '../../shared/infrastructure/base-response';
import {TaskResource} from '../../tasks/infrastructure/tasks.response';
import {RequestStatus, RequestType} from '../domain/model/task-request.entity';

export interface RequestResource extends BaseResource {
  id: number;
  description: string;
  requestType: RequestType;
  requestStatus: RequestStatus;
  task: TaskResource;
}

export interface RequestsResponse extends BaseResponse {
  requests: RequestResource[];
}

// The task id travels in the path, so it is not part of the body.
export interface CreateRequestRequest {
  description: string;
  requestType: RequestType;
}
