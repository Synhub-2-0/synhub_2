import {Injectable} from '@angular/core';
import {BaseApi} from '../../shared/infrastructure/base-api';
import {TasksApiEndpoint} from './tasks.api-endpoint';
import {HttpClient} from '@angular/common/http';
import {Task} from '../domain/model/task.entity';

@Injectable({providedIn: 'root'})
export class TasksApi extends BaseApi {
  private readonly tasksEndpoint: TasksApiEndpoint;

  constructor(http: HttpClient) {
    super();
    this.tasksEndpoint = new TasksApiEndpoint(http);
  }

  // In endpoint, task creation isnt available yet
  createTask(task: Task) {
    return this.tasksEndpoint.create(task);
  }

  getTaskById(taskId: number) {
    return this.tasksEndpoint.getById(taskId);
  }

  updateTask(task: Task) {
    return this.tasksEndpoint.update(task, task.id);
  }

  deleteTask(taskId: number) {
    return this.tasksEndpoint.delete(taskId);
  }

  updateTaskStatus(taskId: number, status: string) {
    return this.tasksEndpoint.updateTaskStatus(taskId, status);
  }

  getTasksByStatus(status: string) {
    return this.tasksEndpoint.getTasksByStatus(status);
  }
}
