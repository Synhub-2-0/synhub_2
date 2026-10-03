import {Routes} from '@angular/router';

const leaderGroups = () => import('./views/leader-groups/leader-groups').then(m => m.LeaderGroups);
const memberGroups = () => import('./views/member-groups/member-groups').then(m => m.MemberGroups);
const groupDetails = () => import('./views/group-details/group-details').then(m => m.GroupDetails);

export const groupsRoutes: Routes = [
  { path: 'leader', loadComponent: leaderGroups },
  { path: 'member', loadComponent: memberGroups },
  { path: '', loadComponent: groupDetails }
];
