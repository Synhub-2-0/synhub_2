import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';
import { Group } from '../../../domain/model/group.entity';

@Component({
  selector: 'app-group-card',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './group-card.html',
  styleUrl: './group-card.css',
})
export class GroupCard {
  readonly fallbackImage = 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=900&q=80';
  @Input({ required: true }) group!: Group;
  @Input() isLeader = false;

  constructor(private readonly router: Router) {}

  openGroupDetails(): void {
    this.router.navigate(['/groups'], { queryParams: { id: this.group.id, role: this.isLeader ? 'leader' : 'member' } }).then();
  }

  openGroupTasks(event: Event): void {
    event.stopPropagation();
    this.router.navigate([this.isLeader ? '/tasks/leader' : '/tasks/member'], { queryParams: { groupId: this.group.id } }).then();
  }
}
