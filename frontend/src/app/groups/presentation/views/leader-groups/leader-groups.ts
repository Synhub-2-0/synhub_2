import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, Router } from '@angular/router';
import { GroupsStore } from '../../../application/groups.store';
import { CreateGroupCommand } from '../../../domain/model/create-group.command';
import { GroupList } from '../../components/group-list/group-list';
import { NoGroupLeader } from '../../components/no-group-leader/no-group-leader';

@Component({
  selector: 'app-leader-groups',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, GroupList, NoGroupLeader],
  templateUrl: './leader-groups.html',
  styleUrl: './leader-groups.css',
})
export class LeaderGroups implements OnInit {
  readonly groupsStore = inject(GroupsStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute, { optional: true });
  readonly showCreateModal = signal(false);
  name = '';
  description = '';
  imgUrl = '';

  ngOnInit(): void {
    this.groupsStore.loadGroups();
    if (this.route?.snapshot.queryParamMap.get('create') === 'true') this.showCreateModal.set(true);
  }

  toggleCreateModal(): void { this.showCreateModal.update(value => !value); }

  onCreateGroup(): void {
    if (!this.name.trim() || !this.description.trim()) return;
    this.groupsStore.addGroup(new CreateGroupCommand({
      name: this.name.trim(),
      description: this.description.trim(),
      imgUrl: this.imgUrl.trim(),
    }), this.router);
    this.showCreateModal.set(false);
    this.name = '';
    this.description = '';
    this.imgUrl = '';
  }
}
