import {BaseAssembler} from '../../shared/infrastructure/base-assembler';
import {Task} from '../domain/model/task.entity';
import {TaskResource, TasksResponse} from './tasks.response';

export class TasksAssembler implements BaseAssembler<Task, TaskResource, TasksResponse> {

  toEntityFromResource(resource: TaskResource): Task {
    return new Task({
      id: resource.id,
      title: resource.title,
      description: resource.description,
      dueDate: new Date(resource.dueDate),
      createdAt: new Date(resource.createdAt),
      updatedAt: new Date(resource.updatedAt),
      status: resource.status
    })
  }

  toEntitiesFromResponse(response: TasksResponse): Task[] {
    return response.tasks.map(resource => this.toEntityFromResource(resource as TaskResource))
  }

  toResourceFromEntity(entity: Task): TaskResource {
    return {
      id: entity.id,
      title: entity.title,
      description: entity.description,
      dueDate: entity.dueDate.toISOString(),
      createdAt: entity.createdAt.toISOString(),
      updatedAt: entity.updatedAt.toISOString(),
      status: entity.status
    } as TaskResource;
  }
}
