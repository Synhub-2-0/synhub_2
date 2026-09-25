import {Component, Input} from '@angular/core';
import {GroupCard} from '../group-card/group-card';

@Component({
  imports: [
    GroupCard
  ],
  selector: 'app-group-list',
  styleUrl: './group-list.css',
  templateUrl: './group-list.html',
})
export class GroupList {
  @Input() groups: {
    // Get groups
  }[] = [];
}
