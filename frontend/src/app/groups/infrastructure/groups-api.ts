import {Injectable} from '@angular/core';
import {BaseApi} from '../../shared/infrastructure/base-api';
import {GroupsApiEndpoint} from './groups.api-endpoint';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Group} from '../domain/model/group.entity';

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

  createGroup(group: Group): Observable<Group> {
    return this.groupsEndpoint.create(group);
  }

  updateGroup(group: Group): Observable<Group> {
    return this.groupsEndpoint.update(group, group.id);
  }

  getGroupsByUser(): Observable<Group[]> {
    return this.groupsEndpoint.getGroupsByUser();
  }
}
