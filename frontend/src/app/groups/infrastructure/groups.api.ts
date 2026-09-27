import {Injectable} from '@angular/core';
import {BaseApi} from '../../shared/infrastructure/base-api';
import {GroupsApiEndpoint} from './groups.api-endpoint';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Group} from '../domain/model/group.entity';
import {CreateGroupCommand} from '../domain/model/create-group.command';

@Injectable({providedIn: 'root'})
export class GroupsApi extends BaseApi {
  private readonly groupsEndpoint: GroupsApiEndpoint;

  constructor(http: HttpClient) {
    super();
    this.groupsEndpoint = new GroupsApiEndpoint(http);
  }

  getGroup(id: number): Observable<Group> {
    return this.groupsEndpoint.getById(id);
  }

  createGroup(createGroupCommand: CreateGroupCommand): Observable<Group> {
    return this.groupsEndpoint.createGroup(createGroupCommand);
  }

  updateGroup(group: Group): Observable<Group> {
    return this.groupsEndpoint.update(group, group.id);
  }

  getGroupsByUser(): Observable<Group[]> {
    return this.groupsEndpoint.getGroupsByUser();
  }

  getLeaderGroups(): Observable<Group[]> {
    return this.groupsEndpoint.getGroupsByUserGroupRole('GROUP_LEADER');
  }

  getMemberGroups(): Observable<Group[]> {
    return this.groupsEndpoint.getGroupsByUserGroupRole('GROUP_MEMBER');
  }

  /*
   * TODO: Reactivar cuando el backend exponga la búsqueda de grupos por código.
   *
   * searchByCode(code: string): Observable<Group> {
   *   return this.groupsEndpoint.searchByCode(code);
   * }
   */

  getGroupsByUserGroupRole(role: 'GROUP_LEADER' | 'GROUP_MEMBER'): Observable<Group[]> {
    return this.groupsEndpoint.getGroupsByUserGroupRole(role);
  }
}
