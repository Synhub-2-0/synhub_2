import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { GroupsStore } from '../../../../groups/application/groups.store';
import { IamStore } from '../../../../iam/application/iam.store';
import { TasksStore } from '../../../../tasks/application/tasks.store';
import { Task, TaskStatus } from '../../../../tasks/domain/model/task.entity';

interface KanbanColumn {
  status: TaskStatus;
  title: string;
  dotClass: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  readonly iamStore = inject(IamStore);
  readonly groupsStore = inject(GroupsStore);
  readonly tasksStore = inject(TasksStore);

  readonly selectedGroupId = signal<number | null>(null);

  readonly columns: KanbanColumn[] = [
    { status: 'IN_PROGRESS', title: 'En progreso', dotClass: 'bg-blue-500' },
    { status: 'ON_HOLD', title: 'En espera', dotClass: 'bg-amber-500' },
    { status: 'COMPLETED', title: 'Completadas', dotClass: 'bg-emerald-500' },
    { status: 'DONE', title: 'Terminadas', dotClass: 'bg-purple-500' },
    { status: 'EXPIRED', title: 'Vencidas', dotClass: 'bg-rose-500' },
  ];

  readonly tasksByStatus = computed(() => {
    const tasks = this.tasksStore.tasks();
    const taskMap: Record<TaskStatus, Task[]> = {
      IN_PROGRESS: [],
      ON_HOLD: [],
      COMPLETED: [],
      DONE: [],
      EXPIRED: [],
    };

    for (const task of tasks) {
      const tasksForStatus = taskMap[task.status];
      if (tasksForStatus) {
        tasksForStatus.push(task);
      }
    }

    return taskMap;
  });

  private readonly autoSelectGroupEffect = effect(() => {
    const groups = this.groupsStore.groups();
    if (groups.length > 0 && this.selectedGroupId() === null) {
      this.selectGroup(groups[0].id);
    }
  });

  ngOnInit(): void {
    this.groupsStore.loadGroups();
  }

  selectGroup(groupId: number): void {
    this.selectedGroupId.set(groupId);
    this.tasksStore.loadTasksByGroup(groupId);
  }

  onChangeTaskStatus(taskId: number, newStatus: TaskStatus): void {
    this.tasksStore.updateTaskStatus(taskId, newStatus);
  }
}
