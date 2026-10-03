import { BaseAssembler } from '../../shared/infrastructure/base-assembler';
import { Task } from '../domain/model/task.entity';
import { CreateTaskCommand } from '../domain/model/create-task.command';
import { UpdateTaskCommand } from '../domain/model/update-task.command';
import {
  CreateTaskRequest,
  TaskResource,
  TasksResponse,
  UpdateTaskRequest,
} from './tasks.response';

export class TasksAssembler implements BaseAssembler<Task, TaskResource, TasksResponse> {
  toEntityFromResource(resource: TaskResource): Task {
    return new Task({
      id: resource.id,
      title: resource.title,
      description: resource.description,
      dueDate: new Date(resource.dueDate),
      createdAt: new Date(resource.createdAt),
      updatedAt: new Date(resource.updatedAt),
      status: resource.status,
      group: resource.group,
      assignedTo: resource.assignedTo,
      difficulty: resource.difficulty,
      context: resource.context,
      urgency: resource.urgency,
      labels: resource.labels,
      aiAccepted: resource.aiAccepted,
    });
  }

  toEntitiesFromResponse(response: TasksResponse): Task[] {
    return response.tasks.map((resource) => this.toEntityFromResource(resource));
  }

  toResourceFromEntity(entity: Task): TaskResource {
    return {
      id: entity.id,
      title: entity.title,
      description: entity.description,
      dueDate: entity.dueDate.toISOString(),
      createdAt: entity.createdAt.toISOString(),
      updatedAt: entity.updatedAt.toISOString(),
      status: entity.status,
      group: entity.group!,
      assignedTo: entity.assignedTo!,
    } as TaskResource;
  }

  toCreateRequestFromCommand(command: CreateTaskCommand): CreateTaskRequest {
    return {
      title: command.title,
      description: command.description,
      dueDate: command.dueDate.toISOString(),
      userId: command.userId,
      groupId: command.groupId,
      difficulty: command.difficulty,
      context: command.context,
      urgency: command.urgency,
      labels: command.labels,
      aiAccepted: command.aiAccepted,
    };
  }

  toUpdateRequestFromCommand(command: UpdateTaskCommand): UpdateTaskRequest {
    return {
      requesterId: command.requesterId,
      title: command.title,
      description: command.description,
      dueDate: command.dueDate.toISOString(),
      userId: command.userId,
    };
  }
}
