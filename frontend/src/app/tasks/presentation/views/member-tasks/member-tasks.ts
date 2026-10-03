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

  // Safeguard: the list is user-scoped on the client too, whatever the endpoint returns.
  readonly myTasks = computed(() =>
    this.tasksStore.filteredTasks().filter((task) => task.assignedTo?.id === this.iamStore.currentUserId()),
  );

  constructor() {
    effect(() => {
      const leaderGroups = this.groupsStore.leaderGroups();
      const userId = this.iamStore.currentUserId();
      if (userId) this.tasksStore.loadAssignedGroupIds(leaderGroups.map((group) => group.id), userId);
    });
    effect(() => {
      const groups = this.availableGroups();
      const userId = this.iamStore.currentUserId();
      const selected = this.selectedGroupId();
      if (groups.length === 0) return;
      if (selected === null) {
        // With several groups the default is the complete picture; with one, that group.
        if (groups.length > 1) this.selectAllGroups(userId);
        else this.selectGroup(groups[0].id, userId);
      } else if (selected === this.ALL_GROUPS) {
        // The list of groups can grow after the led-groups probe finishes.
        this.selectAllGroups(userId);
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
  }

  selectGroup(groupId: number, userId = this.iamStore.currentUserId()): void {
    this.selectedGroupId.set(groupId);
    // This view is personal: without a user id nothing is requested, never the tasks of the whole group.
    if (userId) this.tasksStore.loadTasksByGroupAndUser(groupId, userId);
  }

  selectAllGroups(userId = this.iamStore.currentUserId()): void {
    this.selectedGroupId.set(this.ALL_GROUPS);
    if (userId) this.tasksStore.loadTasksByGroupsAndUser(this.availableGroups().map((group) => group.id), userId);
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
