import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { GroupsStore } from '../../../application/groups.store';
import { Group } from '../../../domain/model/group.entity';
import { TaskStatus } from '../../../../tasks/domain/model/task.entity';
import { TasksStore } from '../../../../tasks/application/tasks.store';
import { TaskList } from '../../../../tasks/presentation/components/task-list/task-list';
import { TaskBoard } from '../../../../tasks/presentation/components/task-board/task-board';
import { IamStore } from '../../../../iam/application/iam.store';
import { InvitationsStore } from '../../../../invitations/application/invitations.store';
import { SubscriptionsStore } from '../../../../subscriptions/application/subscriptions.store';
import { MetricsStore } from '../../../../metrics/application/metrics.store';
import { TaskDistributionChart } from '../../../../metrics/presentation/components/task-distribution-chart/task-distribution-chart';
import { RescheduledChart } from '../../../../metrics/presentation/components/rescheduled-chart/rescheduled-chart';

@Component({
  selector: 'app-group-details',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, RouterLink, TaskList, TaskBoard, TaskDistributionChart, RescheduledChart],
  templateUrl: './group-details.html',
  styleUrl: './group-details.css',
})
export class GroupDetails implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly groupsStore = inject(GroupsStore);
  readonly tasksStore = inject(TasksStore);
  readonly invitationsStore = inject(InvitationsStore);
  readonly iamStore = inject(IamStore);
  readonly subscriptionsStore = inject(SubscriptionsStore);
  readonly metricsStore = inject(MetricsStore);
  readonly groupId = signal(0);
  readonly isEditing = signal(false);
  editName = '';
  editDescription = '';
  editImgUrl = '';
  inviteUsername = '';
  /** Integrante cuya expulsión espera confirmación (id de usuario), o 'self' para salir del grupo. */
  readonly pendingRemoval = signal<number | 'self' | null>(null);

  readonly group = computed<Group | undefined>(() => this.groupsStore.groups().find(group => group.id === this.groupId()));

  // The role is not in the URL: the group is led by the user when it is in the list of groups they lead.
  readonly isLeaderView = computed(() => this.groupsStore.leaderGroups().some(group => group.id === this.groupId()));

  /** A locked group (its leader exceeded the plan limit) is read-only for everyone. */
  readonly isLocked = computed(() => this.group()?.locked ?? false);

  /** The leader can always delete the group, also while it is locked (it is how a slot is freed). */
  readonly canDelete = computed(() => this.isLeaderView());

  /** Delete waits for confirmation: it also deletes the tasks, requests and invitations of the group. */
  readonly confirmingDelete = signal(false);

  /** Only the leader of an active group administers it (edit, invite, remove, manage tasks). */
  readonly canManage = computed(() => this.isLeaderView() && !this.isLocked());

  /** Unknown plan (not loaded yet): the button stays enabled and the backend decides. */
  readonly hasFreeSlot = computed(() => this.subscriptionsStore.subscription()?.hasFreeSlot ?? true);

  // Members are a separate resource (GET /groups/{id}/members), cached by group in the store.
  private readonly groupMembers = computed(() => this.groupsStore.membersByGroup()[this.groupId()] ?? []);
  readonly leader = computed(() => this.groupMembers().find(user => user.roleInGroup === 'GROUP_LEADER')?.user);
  readonly members = computed(() => this.groupMembers()
    .filter(user => user.roleInGroup === 'GROUP_MEMBER')
    .map(user => user.user));

  constructor() {
    // Only the leader sees the invitations: they load once the groups say this user leads the group.
    effect(() => {
      const id = this.groupId();
      if (id && this.isLeaderView()) {
        this.invitationsStore.clearError();
        this.invitationsStore.loadGroupInvitations(id);
        if (!this.subscriptionsStore.subscription()) this.subscriptionsStore.load();
        this.metricsStore.loadGroupDistribution(id);
        this.metricsStore.loadGroupRescheduled(id);
      }
    });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.groupId.set(id);
      this.groupsStore.loadGroupMembers(id);
      this.tasksStore.loadTasksByGroup(id);
    }
  }

  askDelete(): void { this.confirmingDelete.set(true); }
  cancelDelete(): void { this.confirmingDelete.set(false); }

  confirmDelete(): void {
    this.confirmingDelete.set(false);
    if (!this.groupId()) return;
    this.groupsStore.deleteGroup(this.groupId(), () => {
      this.subscriptionsStore.load();
      this.router.navigate(['/groups/leader']).then();
    });
  }

  activateGroup(): void {
    if (!this.groupId()) return;
    this.groupsStore.activateGroup(this.groupId(), () => this.subscriptionsStore.load());
  }

  goToTasks(): void {
    this.router
      .navigate([this.isLeaderView() ? '/tasks/leader' : '/tasks/member'], { queryParams: { groupId: this.groupId() } })
      .then();
  }

  inviteUser(): void {
    const username = this.inviteUsername.trim();
    if (!username || !this.groupId()) return;
    this.invitationsStore.inviteByUsername(username, this.groupId(), () => this.inviteUsername = '');
  }

  askRemoval(target: number | 'self'): void { this.pendingRemoval.set(target); }
  cancelRemoval(): void { this.pendingRemoval.set(null); }

  confirmRemoval(): void {
    const target = this.pendingRemoval();
    this.pendingRemoval.set(null);
    if (!target || !this.groupId()) return;
    if (target === 'self') this.groupsStore.leaveGroup(this.groupId(), this.router);
    else this.groupsStore.removeMember(this.groupId(), target, () => this.tasksStore.loadTasksByGroup(this.groupId()));
  }

  cancelInvitation(invitationId: number): void {
    this.invitationsStore.declineInvitation(invitationId);
  }

  startEdit(): void {
    const group = this.group();
    if (!group) return;
    this.editName = group.name;
    this.editDescription = group.description;
    this.editImgUrl = group.imgUrl;
    this.isEditing.set(true);
  }

  cancelEdit(): void { this.isEditing.set(false); }

  saveEdit(): void {
    const group = this.group();
    if (!group || !this.editName.trim() || !this.editDescription.trim()) return;
    this.groupsStore.updateGroup(new Group({
      id: group.id,
      name: this.editName.trim(),
      description: this.editDescription.trim(),
      imgUrl: this.editImgUrl.trim(),
      code: group.code,
      memberCount: group.memberCount,
    }), this.router);
    this.isEditing.set(false);
  }

  onChangeTaskStatus(event: { taskId: number; status: TaskStatus }): void {
    this.tasksStore.updateTaskStatus(event.taskId, event.status);
  }

  onDeleteTask(taskId: number): void { this.tasksStore.deleteTask(taskId); }
  goBack(): void { this.router.navigate([this.isLeaderView() ? '/groups/leader' : '/groups/member']).then(); }
}
