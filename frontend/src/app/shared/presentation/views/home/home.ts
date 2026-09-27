import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { GroupsStore } from '../../../../groups/application/groups.store';
import { IamStore } from '../../../../iam/application/iam.store';
import { TasksStore } from '../../../../tasks/application/tasks.store';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  readonly iamStore = inject(IamStore);
  readonly groupsStore = inject(GroupsStore);
  readonly tasksStore = inject(TasksStore);

  ngOnInit(): void {
    this.groupsStore.loadGroups();
  }
}
