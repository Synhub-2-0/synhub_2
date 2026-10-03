import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { TasksStore } from '../../../application/tasks.store';
import { GroupsStore } from '../../../../groups/application/groups.store';
import { IamStore } from '../../../../iam/application/iam.store';
import { ToastStore } from '../../../../shared/application/toast.store';
import { CreateTaskCommand } from '../../../domain/model/create-task.command';
import { UpdateTaskCommand } from '../../../domain/model/update-task.command';
import { Task } from '../../../domain/model/task.entity';

@Component({
  selector: 'app-task-form',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './task-form.html',
  styleUrl: './task-form.css',
})
export class TaskForm implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly tasksStore = inject(TasksStore);
  readonly groupsStore = inject(GroupsStore);
  readonly iamStore = inject(IamStore);
  private readonly toastStore = inject(ToastStore);

  isEditMode = false;
  taskId: number | null = null;
  title = '';
  description = '';
  dueDateTime = '';
  groupId: number | null = null;
  userId: number | null = null;
  private readonly activeGroupId = signal<number | null>(null);
  // Every member of the group can be assigned a task, the leader included (listed first).
  readonly members = computed(() => [...(this.groupsStore.membersByGroup()[this.activeGroupId() ?? 0] ?? [])]
    .sort((a, b) => Number(b.roleInGroup === 'GROUP_LEADER') - Number(a.roleInGroup === 'GROUP_LEADER')));

  constructor() {
    effect(() => {
      const task = this.tasksStore.selectedTask();
      if (this.isEditMode && task?.id === this.taskId) this.populateForm(task);
    });
    effect(() => {
      const firstGroup = this.groupsStore.leaderGroups()[0];
      if (!this.isEditMode && !this.groupId && firstGroup) {
        this.groupId = firstGroup.id;
        this.loadGroupMembers(firstGroup.id);
      }
    });
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const queryGroupId = this.route.snapshot.queryParamMap.get('groupId');
    this.groupId = queryGroupId ? Number(queryGroupId) : this.groupsStore.leaderGroups()[0]?.id ?? null;

    if (idParam) {
      this.isEditMode = true;
      this.taskId = Number(idParam);
      const existing = this.tasksStore.getTaskById(this.taskId)();
      if (existing) this.populateForm(existing);
      else this.tasksStore.loadTaskById(this.taskId);
    } else if (this.groupId) {
      this.loadGroupMembers(this.groupId);
    }
  }

  private populateForm(task: Task): void {
    if (task.status === 'DONE') {
      this.toastStore.error('Una tarea terminada no puede editarse ni eliminarse.');
      this.router.navigate(['/tasks/leader']).then();
      return;
    }
    this.title = task.title;
    this.description = task.description;
    this.groupId = task.group?.id ?? this.groupId;
    this.userId = task.assignedTo?.id ?? null;
    const date = new Date(task.dueDate);
    const pad = (value: number) => value.toString().padStart(2, '0');
    this.dueDateTime = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
    if (this.groupId) this.loadGroupMembers(this.groupId);
  }

  onGroupChange(groupId: number | string | null): void {
    const parsedGroupId = Number(groupId);
    if (!Number.isFinite(parsedGroupId)) return;

    this.groupId = parsedGroupId;
    this.userId = null;
    this.loadGroupMembers(this.groupId);
  }

  loadGroupMembers(groupId: number): void {
    this.activeGroupId.set(groupId);
    this.groupsStore.loadGroupMembers(groupId);
  }

  save(): void {
    if (!this.title.trim() || !this.dueDateTime || !this.userId || !this.groupId) return;
    const dueDate = new Date(this.dueDateTime);
    if (this.isEditMode && this.taskId) {
      this.tasksStore.updateTask(this.taskId, new UpdateTaskCommand(
        this.iamStore.currentUserId() ?? 0, this.title.trim(), this.description.trim(), dueDate, this.userId,
      ));
    } else {
      this.tasksStore.addTask(new CreateTaskCommand(
        this.title.trim(), this.description.trim(), dueDate, this.userId, this.groupId,
      ));
    }
    this.router.navigate(['/tasks/leader']).then();
  }

  cancel(): void { this.router.navigate(['/tasks/leader']).then(); }
}
