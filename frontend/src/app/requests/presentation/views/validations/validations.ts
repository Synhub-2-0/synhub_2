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

  readonly typeOptions: { value: RequestType; label: string }[] = [
    { value: 'SUBMISSION', label: 'Entregas' },
    { value: 'MODIFICATION', label: 'Modificaciones' },
    { value: 'EXPIRED', label: 'Vencidas' },
  ];

  /** Each type has its own color so the leader finds it at a glance: submissions green, modifications yellow, expired red. */
  readonly typeChipClass: Record<RequestType, { on: string; off: string }> = {
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

  setType(type: RequestType): void {
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

  // A rejected task goes back to IN_PROGRESS, so a past deadline turns it into EXPIRED right away.
  reject(request: TaskRequest): void {
    if (this.isOverdue(request) && !confirm('La fecha límite ya pasó: si rechazas la entrega, la tarea pasará a Vencida. ¿Deseas continuar?')) {
      return;
    }
    this.requestsStore.rejectRequest(request);
  }

  /** A modification is resolved by editing the task: the backend then closes the request and resumes the task. */
  editTask(request: TaskRequest): void {
    this.router.navigate(['/tasks', request.task.id, 'edit']).then();
  }

  /** The leader dismisses an expiration notice and leaves the task as it is. */
  dismissExpired(request: TaskRequest): void {
    this.requestsStore.rejectRequest(request);
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

  /** Resumes the expired task (new date, then IN_PROGRESS) and closes its expiration request as approved. */
  confirmResume(request: TaskRequest): void {
    if (!this.isResumeDateValid()) return;
    this.tasksStore.resumeExpiredTask(request.task, new Date(this.resumeDate()), () => {
      this.cancelResume();
      this.requestsStore.approveRequest(request);
    });
  }

  initialsOf(request: TaskRequest): string {
    const user = request.task.assignedTo;
    return `${user?.name?.[0] ?? ''}${user?.surname?.[0] ?? ''}`.toUpperCase() || 'U';
  }
}
