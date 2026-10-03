import { CommonModule } from '@angular/common';
import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { GroupsStore } from '../../../../groups/application/groups.store';
import { IamStore } from '../../../../iam/application/iam.store';
import { TasksStore } from '../../../../tasks/application/tasks.store';
import { TaskStatus } from '../../../../tasks/domain/model/task.entity';
import { TaskBoard } from '../../../../tasks/presentation/components/task-board/task-board';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule, TaskBoard],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  readonly iamStore = inject(IamStore);
  readonly groupsStore = inject(GroupsStore);
  readonly tasksStore = inject(TasksStore);

  readonly selectedGroupId = signal<number | null>(null);

  private readonly autoSelectGroupEffect = effect(() => {
    const groups = this.groupsStore.groups();
    if (groups.length > 0 && this.selectedGroupId() === null) {
      this.selectGroup(groups[0].id);
    }
  });

  ngOnInit(): void {
    this.groupsStore.loadGroups();
  }

  selectGroup(groupId: number): void {
    this.selectedGroupId.set(groupId);
    this.tasksStore.loadTasksByGroup(groupId);
  }

  onChangeTaskStatus(taskId: number, newStatus: TaskStatus): void {
    this.tasksStore.updateTaskStatus(taskId, newStatus);
  }
}
