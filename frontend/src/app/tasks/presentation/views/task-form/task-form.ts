import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TasksStore } from '../../../application/tasks.store';
import { GroupsStore } from '../../../../groups/application/groups.store';
import { IamStore } from '../../../../iam/application/iam.store';
import { ToastStore } from '../../../../shared/application/toast.store';
import { CreateTaskCommand } from '../../../domain/model/create-task.command';
import { UpdateTaskCommand } from '../../../domain/model/update-task.command';
import { Task } from '../../../domain/model/task.entity';
import { AiStore } from '../../../../ai/application/ai.store';
import { DIFFICULTY_SCALE, TASK_CONTEXTS, TASK_URGENCIES } from '../../../../ai/domain/model/task-classification.entity';

@Component({
  selector: 'app-task-form',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatTooltipModule],
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
  readonly aiStore = inject(AiStore);

  isEditMode = false;
  taskId: number | null = null;
  title = '';
  description = '';
  dueDateTime = '';
  groupId: number | null = null;
  userId: number | null = null;
  // Sugerencia de IA (editable). aiStore.classification() conserva el original para medir la aceptación.
  difficulty: number | null = null;
  context = '';
  urgency = '';
  labels = '';
  readonly contexts = TASK_CONTEXTS;
  readonly urgencies = TASK_URGENCIES;
  readonly difficulties = [1, 2, 3, 4, 5];
  readonly difficultyScale = DIFFICULTY_SCALE;
  private readonly activeGroupId = signal<number | null>(null);
  // Every member of the group can be assigned a task, the leader included (listed first).
  readonly members = computed(() => [...(this.groupsStore.membersByGroup()[this.activeGroupId() ?? 0] ?? [])]
    .sort((a, b) => Number(b.roleInGroup === 'GROUP_LEADER') - Number(a.roleInGroup === 'GROUP_LEADER')));

  constructor() {
    effect(() => {
      const suggestion = this.aiStore.classification();
      if (!suggestion) return;
      this.difficulty = suggestion.difficulty;
      this.context = suggestion.context;
      this.urgency = suggestion.urgency;
      this.labels = suggestion.labels.join(', ');
    });
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
    this.aiStore.clearClassification();
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

  suggest(): void {
    if (!this.title.trim()) return;
    this.aiStore.classifyTask({
      title: this.title.trim(),
      description: this.description.trim(),
      dueDate: this.dueDateTime ? new Date(this.dueDateTime) : undefined,
    });
  }

  /** true si se guardó la sugerencia tal cual, false si se editó, undefined si no se usó la IA. */
  private get aiAccepted(): boolean | undefined {
    const s = this.aiStore.classification();
    if (!s) return undefined;
    return s.difficulty === this.difficulty && s.context === this.context &&
      s.urgency === this.urgency && s.labels.join(', ') === this.labels.trim();
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
        this.difficulty ?? undefined, this.context || undefined, this.urgency || undefined,
        this.labels.trim() || undefined, this.aiAccepted,
      ));
    }
    this.router.navigate(['/tasks/leader']).then();
  }

  cancel(): void { this.router.navigate(['/tasks/leader']).then(); }
}
