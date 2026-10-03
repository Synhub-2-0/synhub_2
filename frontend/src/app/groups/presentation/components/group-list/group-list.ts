import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { Group } from '../../../domain/model/group.entity';
import { GroupCard } from '../group-card/group-card';

@Component({
  selector: 'app-group-list',
  standalone: true,
  imports: [CommonModule, GroupCard],
  templateUrl: './group-list.html',
  styleUrl: './group-list.css',
})
export class GroupList {
  @Input() groups: Group[] = [];
  @Input() isLeader = false;
}
