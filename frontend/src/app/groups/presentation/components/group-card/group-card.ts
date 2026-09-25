import {Component, Input} from '@angular/core';
import {Group} from '../../../domain/model/group.entity';

@Component({
  imports: [],
  selector: 'app-group-card',
  styleUrl: './group-card.css',
  templateUrl: './group-card.html',
})
export class GroupCard {
  // TODO: Build group entity
    @Input() groupInfo: Group | null = null;
}
