import { CommonModule } from '@angular/common';
import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { GroupsStore } from '../../../../groups/application/groups.store';
import { RequestsStore } from '../../../application/requests.store';
import { RequestStatus, TaskRequest } from '../../../domain/model/task-request.entity';

@Component({
  selector: 'app-validations',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule],
  templateUrl: './validations.html',
  styleUrl: './validations.css',
})
export class Validations implements OnInit {
  private readonly route = inject(ActivatedRoute);
  readonly groupsStore = inject(GroupsStore);
  readonly requestsStore = inject(RequestsStore);

  readonly selectedGroupId = signal<number | null>(null);

  readonly statusOptions: { value: RequestStatus; label: string }[] = [
    { value: 'PENDING', label: 'Pendientes' },
    { value: 'APPROVED', label: 'Aprobadas' },
    { value: 'REJECTED', label: 'Rechazadas' },
  ];

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
    this.requestsStore.loadGroupSubmissions(groupId);
  }

  setStatus(status: RequestStatus): void {
    const groupId = this.selectedGroupId();
    if (groupId) this.requestsStore.loadGroupSubmissions(groupId, status);
  }

  isOverdue(request: TaskRequest): boolean {
    if (!request?.task?.dueDate) return false;
    return new Date(request.task.dueDate).getTime() < Date.now();
  }

  approve(request: TaskRequest): void {
    this.requestsStore.approveSubmission(request);
  }

  // A rejected task goes back to IN_PROGRESS, so a past deadline turns it into EXPIRED right away.
  reject(request: TaskRequest): void {
    if (this.isOverdue(request) && !confirm('La fecha límite ya pasó: si rechazas la entrega, la tarea pasará a Vencida. ¿Deseas continuar?')) {
      return;
    }
    this.requestsStore.rejectSubmission(request);
  }

  initialsOf(request: TaskRequest): string {
    const user = request.task.assignedTo;
    return `${user?.name?.[0] ?? ''}${user?.surname?.[0] ?? ''}`.toUpperCase() || 'U';
  }
}
