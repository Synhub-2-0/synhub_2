import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { GroupsStore } from '../../../application/groups.store';
import { Group } from '../../../domain/model/group.entity';
import { TaskStatus } from '../../../../tasks/domain/model/task.entity';
import { TasksStore } from '../../../../tasks/application/tasks.store';
import { TaskList } from '../../../../tasks/presentation/components/task-list/task-list';
import { TaskBoard } from '../../../../tasks/presentation/components/task-board/task-board';
import { IamStore } from '../../../../iam/application/iam.store';
import { InvitationsStore } from '../../../../invitations/application/invitations.store';

@Component({
  selector: 'app-group-details',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, TaskList, TaskBoard],
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
  readonly groupId = signal(0);
  readonly isLeaderView = signal(false);
  readonly isEditing = signal(false);
  editName = '';
  editDescription = '';
  editImgUrl = '';
  inviteUsername = '';

  readonly group = computed<Group | undefined>(() => {
    const groups = this.groupsStore.groups();
    return this.groupId() ? groups.find(group => group.id === this.groupId()) : groups[0];
  });

  // Members are a separate resource (GET /groups/{id}/members), cached by group in the store.
  private readonly groupMembers = computed(() => this.groupsStore.membersByGroup()[this.groupId()] ?? []);
  readonly leader = computed(() => this.groupMembers().find(user => user.roleInGroup === 'GROUP_LEADER')?.user);
  readonly members = computed(() => this.groupMembers()
    .filter(user => user.roleInGroup === 'GROUP_MEMBER')
    .map(user => user.user));

  ngOnInit(): void {
    const id = Number(this.route.snapshot.queryParamMap.get('id'));
    const role = this.route.snapshot.queryParamMap.get('role');
    this.isLeaderView.set(role === 'leader');
    const selectedId = id || this.groupsStore.groups()[0]?.id;
    if (selectedId) {
      this.groupId.set(selectedId);
      this.groupsStore.loadGroupMembers(selectedId);
      this.tasksStore.loadTasksByGroup(selectedId);
      if (this.isLeaderView()) {
        this.invitationsStore.clearError();
        this.invitationsStore.loadGroupInvitations(selectedId);
      }
    }
  }

  inviteUser(): void {
    const username = this.inviteUsername.trim();
    if (!username || !this.groupId()) return;
    this.invitationsStore.inviteByUsername(username, this.groupId(), () => this.inviteUsername = '');
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
