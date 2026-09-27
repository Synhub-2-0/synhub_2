import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { TasksStore } from '../../../application/tasks.store';
import { GroupsStore } from '../../../../groups/application/groups.store';
import { IamStore } from '../../../../iam/application/iam.store';
import { TaskStatus } from '../../../domain/model/task.entity';
import { TaskList } from '../../components/task-list/task-list';

@Component({
  selector: 'app-member-tasks',
  standalone: true,
  imports: [CommonModule, MatIconModule, TaskList],
  templateUrl: './member-tasks.html',
  styleUrl: './member-tasks.css',
})
export class MemberTasks implements OnInit {
  private readonly route = inject(ActivatedRoute);
  readonly tasksStore = inject(TasksStore);
  readonly groupsStore = inject(GroupsStore);
  readonly iamStore = inject(IamStore);

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
      const groups = this.groupsStore.memberGroups();
      const userId = this.iamStore.currentUserId();
      if (groups.length > 0 && !this.selectedGroupId()) {
        this.selectGroup(groups[0].id, userId);
      }
    });
  }

  ngOnInit(): void {
    if (this.groupsStore.groups().length === 0) {
      this.groupsStore.loadGroups();
    }

    const queryGroupId = Number(this.route.snapshot.queryParamMap.get('groupId'));
    const userId = this.iamStore.currentUserId();

    if (queryGroupId) {
      this.selectGroup(queryGroupId, userId);
      return;
    }

    const groups = this.groupsStore.memberGroups();
    if (groups.length > 0) {
      this.selectGroup(groups[0].id, userId);
    } else {
      // Fallback directo al Grupo 1 para cargar tareas aunque falle /groups/user/role
      this.selectGroup(1, userId);
    }
  }

  selectGroup(groupId: number, userId = this.iamStore.currentUserId()): void {
    this.selectedGroupId.set(groupId);
    if (userId) {
      this.tasksStore.loadTasksByGroupAndUser(groupId, userId);
    } else {
      this.tasksStore.loadTasksByGroup(groupId);
    }
  }

  setFilter(status: TaskStatus | 'ALL'): void {
    this.tasksStore.setStatusFilter(status);
  }

  labelFor(status: TaskStatus | 'ALL'): string {
    return this.statusLabels[status];
  }

  onChangeStatus(event: { taskId: number; status: TaskStatus }): void {
    this.tasksStore.updateTaskStatus(event.taskId, event.status);
  }
}
