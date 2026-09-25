import {Component, signal} from '@angular/core';
import {GroupList} from '../../components/group-list/group-list';

@Component({
  imports: [
    GroupList
  ],
  selector: 'app-member-groups',
  styleUrl: './member-groups.css',
  templateUrl: './member-groups.html',
})
export class MemberGroups {
  // TODO: Add group endpoint filtered for members
  groups = signal([

  ])
}
