import {Component, Input} from '@angular/core';

@Component({
  imports: [],
  selector: 'app-group-card',
  styleUrl: './group-card.css',
  templateUrl: './group-card.html',
})
export class GroupCard {
  // TODO: Build group entity
    @Input() groupInfo: {} = undefined;
}
