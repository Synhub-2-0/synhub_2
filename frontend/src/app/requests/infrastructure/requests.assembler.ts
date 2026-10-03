import {BaseAssembler} from '../../shared/infrastructure/base-assembler';
import {TaskRequest} from '../domain/model/task-request.entity';
import {CreateRequestCommand} from '../domain/model/create-request.command';
import {CreateRequestRequest, RequestResource, RequestsResponse} from './requests.response';
import {TasksAssembler} from '../../tasks/infrastructure/tasks.assembler';

export class RequestsAssembler implements BaseAssembler<TaskRequest, RequestResource, RequestsResponse> {
  private taskAssembler = new TasksAssembler();

  toEntitiesFromResponse(response: RequestsResponse): TaskRequest[] {
    return (response?.requests ?? []).map(resource => this.toEntityFromResource(resource));
  }

  toEntityFromResource(resource: RequestResource): TaskRequest {
    return new TaskRequest({
      id: resource.id,
      description: resource.description,
      requestType: resource.requestType,
      requestStatus: resource.requestStatus,
      task: this.taskAssembler.toEntityFromResource(resource.task)
    });
  }

  toResourceFromEntity(entity: TaskRequest): RequestResource {
    return {
      id: entity.id,
      description: entity.description,
      requestType: entity.requestType,
      requestStatus: entity.requestStatus,
      task: this.taskAssembler.toResourceFromEntity(entity.task)
    } as RequestResource;
  }

  toRequestFromCreateCommand(command: CreateRequestCommand): CreateRequestRequest {
    return {
      description: command.description,
      requestType: command.requestType
    } as CreateRequestRequest;
  }
}
