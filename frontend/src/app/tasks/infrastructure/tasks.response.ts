import {BaseResource} from '../../shared/infrastructure/base-response';

export interface TaskResource extends BaseResource {
  id: number;
  title: string;
  description: string;
  dueDate: string;
  createdAt: string;
  updatedAt: string;
  status: string;
}
export interface TasksResponse {
  tasks: TaskResource[];
}
