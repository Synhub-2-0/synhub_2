import { BaseResource } from '../../shared/infrastructure/base-response';
import { TaskStatus } from '../domain/model/task.entity';

export interface TaskUserResource {
  id: number;
  username: string;
  name: string;
  surname: string;
  imgUrl: string;
  email: string;
  accountRoles: string[];
}

export interface TaskGroupReducedResource {
  id: number;
  name: string;
  imgUrl: string;
  description: string;
  code: string;
  memberCount: number;
  leader: TaskUserResource;
}

export interface TaskResource extends BaseResource {
  id: number;
  title: string;
  description: string;
  dueDate: string;
  createdAt: string;
  updatedAt: string;
  status: TaskStatus;
  group: TaskGroupReducedResource;
  assignedTo: TaskUserResource;
}

export interface TasksResponse {
  tasks: TaskResource[];
}

export interface CreateTaskRequest {
  title: string;
  description: string;
  dueDate: string;
  userId: number;
  groupId: number;
}

export interface UpdateTaskRequest {
  requesterId: number;
  title: string;
  description: string;
  dueDate: string;
  userId: number;
}
