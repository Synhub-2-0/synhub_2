import { Injectable } from '@angular/core';
import { TasksApiEndpoint } from './tasks.api-endpoint';
import { HttpClient } from '@angular/common/http';
import { Task, TaskStatus } from '../domain/model/task.entity';
import { CreateTaskCommand } from '../domain/model/create-task.command';
import { UpdateTaskCommand } from '../domain/model/update-task.command';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class TasksApi {
  private readonly tasksEndpoint: TasksApiEndpoint;

  constructor(http: HttpClient) {
    this.tasksEndpoint = new TasksApiEndpoint(http);
  }

  createTask(command: CreateTaskCommand): Observable<Task> {
    return this.tasksEndpoint.createTask(command);
  }

  getTaskById(taskId: number): Observable<Task> {
    return this.tasksEndpoint.getById(taskId);
  }

  updateTask(taskId: number, command: UpdateTaskCommand): Observable<Task> {
    return this.tasksEndpoint.updateTaskDetails(taskId, command);
  }

  deleteTask(taskId: number): Observable<void> {
    return this.tasksEndpoint.delete(taskId);
  }

  updateTaskStatus(taskId: number, status: TaskStatus): Observable<Task> {
    return this.tasksEndpoint.updateTaskStatus(taskId, status);
  }

  getTasksByGroup(groupId: number, status?: TaskStatus): Observable<Task[]> {
    return this.tasksEndpoint.getTasksByGroup(groupId, status);
  }

  getMyTasks(): Observable<Task[]> {
    return this.tasksEndpoint.getMyTasks();
  }

  getTasksByGroupAndUser(groupId: number, userId: number): Observable<Task[]> {
    return this.tasksEndpoint.getTasksByGroupAndUser(groupId, userId);
  }
}
