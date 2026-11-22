import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { Group } from '../../../domain/model/group.entity';
import { GroupsStore } from '../../../application/groups.store';
import { SubscriptionsStore } from '../../../../subscriptions/application/subscriptions.store';

@Component({
  selector: 'app-group-card',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './group-card.html',
  styleUrl: './group-card.css',
})
export class GroupCard implements OnInit {
  private readonly router = inject(Router);
  private readonly groupsStore = inject(GroupsStore);
  private readonly subscriptionsStore = inject(SubscriptionsStore);

  readonly fallbackImage =
    'https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=900&q=80';
  @Input({ required: true }) group!: Group;
  @Input() isLeader = false;

  ngOnInit(): void {
    // The leader of a locked group needs to know if there is a free slot to activate it.
    if (this.isLeader && this.group.locked && !this.subscriptionsStore.subscription()) {
      this.subscriptionsStore.load();
    }
  }

  /** Unknown plan (not loaded yet): the button stays enabled and the backend decides. */
  get hasFreeSlot(): boolean {
    return this.subscriptionsStore.subscription()?.hasFreeSlot ?? true;
  }

  activateGroup(event: Event): void {
    event.stopPropagation();
    this.groupsStore.activateGroup(this.group.id, () => this.subscriptionsStore.load());
  }

  openGroupDetails(): void {
    this.router.navigate(['/groups', this.group.id]).then();
  }

  openGroupTasks(event: Event): void {
    event.stopPropagation();
    this.router
      .navigate([this.isLeader ? '/tasks/leader' : '/tasks/member'], {
        queryParams: { groupId: this.group.id },
      })
      .then();
  }
}
