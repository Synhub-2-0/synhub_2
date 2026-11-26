import { Component, Input, OnChanges } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { GroupUser } from '../../../../groups/domain/model/group-user.entity';
import { Task } from '../../../../tasks/domain/model/task.entity';

interface MemberWorkload {
  id: number;
  name: string;
  assignedTasks: number;
  allocationPercent: number;
  expiredTasks: number;
  inProgressTasks: number;
  completedTasks: number;
}

@Component({
  selector: 'app-member-workload-breakdown',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './member-workload-breakdown.html',
})
export class MemberWorkloadBreakdown implements OnChanges {
  @Input() members: GroupUser[] = [];
  @Input() tasks: Task[] = [];
  @Input() totalTasks: number | null = null;
  @Input() loading = false;
  @Input() error: string | null = null;

  workloads: MemberWorkload[] = [];

  ngOnChanges(): void {
    this.buildWorkloads();
  }

  private buildWorkloads(): void {
    this.workloads = this.members.map(({ user }) => {
      const assigned = this.tasks.filter((task) => task.assignedTo?.id === user.id);
      const assignedTasks = assigned.length;
      return {
        id: user.id,
        name: `${user.name} ${user.surname}`.trim(),
        assignedTasks,
        allocationPercent: this.totalTasks && this.totalTasks > 0 ? (assignedTasks / this.totalTasks) * 100 : 0,
        expiredTasks: assigned.filter((task) => task.status === 'EXPIRED').length,
        inProgressTasks: assigned.filter(
          (task) => task.status === 'IN_PROGRESS' || task.status === 'ON_HOLD' || task.status === 'COMPLETED',
        ).length,
        completedTasks: assigned.filter((task) => task.status === 'DONE').length,
      };
    });
  }
}
