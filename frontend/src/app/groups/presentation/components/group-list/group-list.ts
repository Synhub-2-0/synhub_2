import {Component, Input} from '@angular/core';
import {GroupCard} from '../group-card/group-card';
import {Group} from '../../../domain/model/group.entity';

@Component({
  imports: [
    GroupCard
  ],
  selector: 'app-group-list',
  styleUrl: './group-list.css',
  templateUrl: './group-list.html',
})
export class GroupList {
  @Input() groups: Group[] = [];
}
