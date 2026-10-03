import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { Task, TaskStatus } from '../../../domain/model/task.entity';
import { TaskCard } from '../task-card/task-card';

@Component({
  selector: 'app-task-list',
  standalone: true,
  imports: [CommonModule, MatIconModule, TaskCard],
  templateUrl: './task-list.html',
  styleUrl: './task-list.css',
})
export class TaskList {
  @Input() tasks: Task[] = [];
  @Input() isLeader = false;
  @Output() deleteTask = new EventEmitter<number>();
  @Output() changeStatus = new EventEmitter<{ taskId: number; status: TaskStatus }>();
}
