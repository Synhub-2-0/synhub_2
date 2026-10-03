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
  difficulty?: number | null;
  context?: string | null;
  urgency?: string | null;
  labels?: string | null;
  aiAccepted?: boolean | null;
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
  difficulty?: number;
  context?: string;
  urgency?: string;
  labels?: string;
  aiAccepted?: boolean;
}

export interface UpdateTaskRequest {
  requesterId: number;
  title: string;
  description: string;
  dueDate: string;
  userId: number;
}
