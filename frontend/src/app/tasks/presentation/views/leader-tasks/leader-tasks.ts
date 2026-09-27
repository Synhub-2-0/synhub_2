import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { TasksStore } from '../../../application/tasks.store';
import { GroupsStore } from '../../../../groups/application/groups.store';
import { TaskStatus } from '../../../domain/model/task.entity';
import { TaskList } from '../../components/task-list/task-list';

@Component({
  selector: 'app-leader-tasks',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule, TaskList],
  templateUrl: './leader-tasks.html',
  styleUrl: './leader-tasks.css',
})
export class LeaderTasks implements OnInit {
  private readonly route = inject(ActivatedRoute);
  readonly tasksStore = inject(TasksStore);
  readonly groupsStore = inject(GroupsStore);

  selectedGroupId = signal<number | null>(null);

  readonly statusOptions: (TaskStatus | 'ALL')[] = [
    'ALL',
    'IN_PROGRESS',
    'ON_HOLD',
    'COMPLETED',
    'DONE',
    'EXPIRED',
  ];

  private readonly statusLabels: Record<TaskStatus | 'ALL', string> = {
    ALL: 'Todas',
    IN_PROGRESS: 'En progreso',
    ON_HOLD: 'En espera',
    COMPLETED: 'Completadas',
    DONE: 'Terminadas',
    EXPIRED: 'Vencidas',
  };

  constructor() {
    effect(() => {
      const groups = this.groupsStore.leaderGroups();
      if (groups.length > 0 && !this.selectedGroupId()) {
        this.selectGroup(groups[0].id);
      }
    });
  }

  ngOnInit(): void {
    if (this.groupsStore.groups().length === 0) {
      this.groupsStore.loadGroups();
    }

    const queryGroupId = Number(this.route.snapshot.queryParamMap.get('groupId'));
    if (queryGroupId) {
      this.selectGroup(queryGroupId);
      return;
    }

    const groups = this.groupsStore.leaderGroups();
    if (groups.length > 0) {
      this.selectGroup(groups[0].id);
    } else {
      // Fallback directo al Grupo 1
      this.selectGroup(1);
    }
  }

  selectGroup(groupId: number): void {
    this.selectedGroupId.set(groupId);
    this.tasksStore.loadTasksByGroup(groupId);
  }

  setFilter(status: TaskStatus | 'ALL'): void {
    this.tasksStore.setStatusFilter(status);
  }

  labelFor(status: TaskStatus | 'ALL'): string {
    return this.statusLabels[status];
  }

  onDeleteTask(taskId: number): void {
    if (confirm('¿Estás seguro de que deseas eliminar esta tarea?')) {
      this.tasksStore.deleteTask(taskId);
    }
  }

  onChangeStatus(event: { taskId: number; status: TaskStatus }): void {
    this.tasksStore.updateTaskStatus(event.taskId, event.status);
  }
}
