import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
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

  // null: nothing chosen yet, ALL_GROUPS: the tasks of every group at once, otherwise a group id.
  readonly ALL_GROUPS = 0;
  selectedGroupId = signal<number | null>(null);

  // Groups where the user is a member, plus the groups he leads only when he also has tasks assigned there.
  readonly availableGroups = computed(() => {
    const memberGroups = this.groupsStore.memberGroups();
    const ledWithTasks = this.groupsStore.leaderGroups().filter(
      (group) => this.tasksStore.assignedGroupIds().includes(group.id) && !memberGroups.some((m) => m.id === group.id),
    );
    return [...memberGroups, ...ledWithTasks];
  });

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

  // Mis tareas (una sola petición, AUDIT PERF-03), filtradas en el cliente por estado y por el grupo elegido.
  // Safeguard: the list is user-scoped on the client too, whatever the endpoint returns.
  readonly myTasks = computed(() => {
    const selected = this.selectedGroupId();
    return this.tasksStore.filteredTasks().filter((task) =>
      task.assignedTo?.id === this.iamStore.currentUserId()
      && (selected === null || selected === this.ALL_GROUPS || task.group?.id === selected));
  });

  constructor() {
    effect(() => {
      const groups = this.availableGroups();
      if (groups.length === 0 || this.selectedGroupId() !== null) return;
      // With several groups the default is the complete picture; with one, that group.
      if (groups.length > 1) this.selectAllGroups();
      else this.selectGroup(groups[0].id);
    });
  }

  ngOnInit(): void {
    if (this.groupsStore.groups().length === 0) {
      this.groupsStore.loadGroups();
    }
    // This view is personal: without a session nothing is requested.
    if (this.iamStore.currentUserId()) this.tasksStore.loadMyTasks();

    const queryGroupId = Number(this.route.snapshot.queryParamMap.get('groupId'));
    if (queryGroupId) this.selectGroup(queryGroupId);
  }

  selectGroup(groupId: number): void {
    this.selectedGroupId.set(groupId);
  }

  selectAllGroups(): void {
    this.selectedGroupId.set(this.ALL_GROUPS);
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
