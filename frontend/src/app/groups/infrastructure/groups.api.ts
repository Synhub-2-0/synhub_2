import {Injectable} from '@angular/core';
import {GroupsApiEndpoint} from './groups.api-endpoint';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Group} from '../domain/model/group.entity';
import {CreateGroupCommand} from '../domain/model/create-group.command';
import {GroupUser} from '../domain/model/group-user.entity';

@Injectable({providedIn: 'root'})
export class GroupsApi {
  private readonly groupsEndpoint: GroupsApiEndpoint;

  constructor(http: HttpClient) {
    this.groupsEndpoint = new GroupsApiEndpoint(http);
  }

  getGroupMembers(groupId: number): Observable<GroupUser[]> {
    return this.groupsEndpoint.getGroupMembers(groupId);
  }

  createGroup(createGroupCommand: CreateGroupCommand): Observable<Group> {
    return this.groupsEndpoint.createGroup(createGroupCommand);
  }

  updateGroup(group: Group): Observable<Group> {
    return this.groupsEndpoint.update(group, group.id);
  }

  getLeaderGroups(): Observable<Group[]> {
    return this.groupsEndpoint.getGroupsByUserGroupRole('GROUP_LEADER');
  }

  getMemberGroups(): Observable<Group[]> {
    return this.groupsEndpoint.getGroupsByUserGroupRole('GROUP_MEMBER');
  }

  joinGroupByCode(code: string): Observable<Group> {
    return this.groupsEndpoint.joinByCode(code);
  }

  removeMember(groupId: number, userId: number): Observable<void> {
    return this.groupsEndpoint.removeMember(groupId, userId);
  }

}
