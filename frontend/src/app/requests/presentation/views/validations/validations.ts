import { CommonModule } from '@angular/common';
import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { GroupsStore } from '../../../../groups/application/groups.store';
import { TasksStore } from '../../../../tasks/application/tasks.store';
import { RequestsStore } from '../../../application/requests.store';
import { RequestStatus, RequestType, TaskRequest } from '../../../domain/model/task-request.entity';

@Component({
  selector: 'app-validations',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule],
  templateUrl: './validations.html',
  styleUrl: './validations.css',
})
export class Validations implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly groupsStore = inject(GroupsStore);
  readonly requestsStore = inject(RequestsStore);
  private readonly tasksStore = inject(TasksStore);

  readonly selectedGroupId = signal<number | null>(null);
  /** Expired request being resumed (the leader picks a new due date first) and the date typed so far. */
  readonly resumingRequestId = signal<number | null>(null);
  readonly resumeDate = signal('');

  readonly statusOptions: { value: RequestStatus; label: string }[] = [
    { value: 'PENDING', label: 'Pendientes' },
    { value: 'APPROVED', label: 'Aprobadas' },
    { value: 'REJECTED', label: 'Rechazadas' },
  ];

  readonly typeOptions: { value: RequestType | 'ALL'; label: string }[] = [
    { value: 'ALL', label: 'Todas' },
    { value: 'SUBMISSION', label: 'Entregas' },
    { value: 'MODIFICATION', label: 'Modificaciones' },
    { value: 'EXPIRED', label: 'Vencidas' },
  ];

  /** Each type has its own color so the leader finds it at a glance: submissions green, modifications yellow, expired red. */
  readonly typeChipClass: Record<RequestType | 'ALL', { on: string; off: string }> = {
    ALL: {
      on: 'bg-slate-800 text-white border-slate-800',
      off: 'bg-white text-slate-600 border-slate-200 hover:bg-slate-100',
    },
    SUBMISSION: {
      on: 'bg-emerald-600 text-white border-emerald-600',
      off: 'bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100',
    },
    MODIFICATION: {
      on: 'bg-amber-400 text-slate-900 border-amber-400',
      off: 'bg-amber-50 text-amber-700 border-amber-200 hover:bg-amber-100',
    },
    EXPIRED: {
      on: 'bg-red-600 text-white border-red-600',
      off: 'bg-red-50 text-red-700 border-red-200 hover:bg-red-100',
    },
  };

  readonly typeBadgeClass: Record<RequestType, string> = {
    SUBMISSION: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    MODIFICATION: 'bg-amber-50 text-amber-700 border-amber-200',
    EXPIRED: 'bg-red-50 text-red-700 border-red-200',
  };

  readonly typeLabels: Record<RequestType, string> = {
    SUBMISSION: 'Entrega',
    MODIFICATION: 'Modificación',
    EXPIRED: 'Vencida',
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
    if (queryGroupId) this.selectGroup(queryGroupId);
  }

  selectGroup(groupId: number): void {
    this.selectedGroupId.set(groupId);
    this.cancelResume();
    this.requestsStore.loadGroupRequests(groupId);
  }

  setStatus(status: RequestStatus): void {
    const groupId = this.selectedGroupId();
    if (groupId) this.requestsStore.loadGroupRequests(groupId, status);
  }

  setType(type: RequestType | 'ALL'): void {
    const groupId = this.selectedGroupId();
    this.cancelResume();
    if (groupId) this.requestsStore.loadGroupRequests(groupId, undefined, type);
  }

  /** The requests of a locked group (its leader exceeded the plan limit) cannot be handled until it is activated. */
  isLocked(request: TaskRequest): boolean {
    return request.task?.group?.locked ?? false;
  }

  isOverdue(request: TaskRequest): boolean {
    if (!request?.task?.dueDate) return false;
    return new Date(request.task.dueDate).getTime() < Date.now();
  }

  approve(request: TaskRequest): void {
    this.requestsStore.approveRequest(request);
  }

  // A rejected submission, or a denied modification, puts the task back in IN_PROGRESS: with a past deadline it turns
  // into EXPIRED right away.
  reject(request: TaskRequest): void {
    if (this.isOverdue(request) && !confirm(this.overdueRejectMessage(request))) {
      return;
    }
    this.requestsStore.rejectRequest(request);
  }

  private overdueRejectMessage(request: TaskRequest): string {
    return request.requestType === 'MODIFICATION'
      ? 'La fecha límite ya pasó: si rechazas la solicitud, la tarea volverá a En progreso y pasará a Vencida. ¿Deseas continuar?'
      : 'La fecha límite ya pasó: si rechazas la entrega, la tarea pasará a Vencida. ¿Deseas continuar?';
  }

  /**
   * A modification is approved by editing the task: the backend then closes the request and resumes the task. After
   * saving or cancelling, the leader comes back to this screen (same group).
   */
  editTask(request: TaskRequest): void {
    const returnTo = `/validations?groupId=${this.selectedGroupId() ?? request.task.group?.id ?? ''}`;
    this.router.navigate(['/tasks', request.task.id, 'edit'], { queryParams: { returnTo } }).then();
  }

  startResume(request: TaskRequest): void {
    this.resumingRequestId.set(request.id);
    this.resumeDate.set('');
  }

  cancelResume(): void {
    this.resumingRequestId.set(null);
    this.resumeDate.set('');
  }

  onResumeDateInput(event: Event): void {
    this.resumeDate.set((event.target as HTMLInputElement).value);
  }

  /** A new due date in the future, so the task does not expire again in a minute. */
  isResumeDateValid(): boolean {
    const value = this.resumeDate();
    return !!value && new Date(value).getTime() > Date.now();
  }

  /** Resumes the expired task (new date, then IN_PROGRESS): the backend closes its expiration notice. */
  confirmResume(request: TaskRequest): void {
    if (!this.isResumeDateValid()) return;
    this.tasksStore.resumeExpiredTask(request.task, new Date(this.resumeDate()), () => {
      this.cancelResume();
      const groupId = this.selectedGroupId();
      if (groupId) this.requestsStore.loadGroupRequests(groupId);
    });
  }

  initialsOf(request: TaskRequest): string {
    const user = request.task.assignedTo;
    return `${user?.name?.[0] ?? ''}${user?.surname?.[0] ?? ''}`.toUpperCase() || 'U';
  }
}
