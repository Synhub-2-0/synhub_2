import {BaseApiEndpoint} from '../../shared/infrastructure/base-api-endpoint';
import {Group} from '../domain/model/group.entity';
import {GroupResource, GroupsResponse, GroupUsersResource} from './groups.response';
import {GroupsAssembler} from './groups.assembler';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';
import {catchError, map, Observable} from 'rxjs';
import {CreateGroupCommand} from '../domain/model/create-group.command';
import {GroupUser} from '../domain/model/group-user.entity';

const groupsEndpointUrl = `${environment.platformProviderApiBaseUrl}${environment.platformProviderGroupsEndpointPath}`;

export class GroupsApiEndpoint extends BaseApiEndpoint<Group, GroupResource, GroupsResponse, GroupsAssembler> {
  constructor(http: HttpClient) {
    super(http, groupsEndpointUrl, new GroupsAssembler());
  }

  createGroup(createGroupCommand: CreateGroupCommand): Observable<Group> {
    const createGroupRequest = this.assembler.toRequestFromCreateCommand(createGroupCommand);
    return this.http.post<GroupResource>(groupsEndpointUrl, createGroupRequest).pipe(
      map(resource => this.assembler.toEntityFromResource(resource)),
      catchError(this.handleError('Failed to create group'))
    );
  }

  getGroupMembers(groupId: number): Observable<GroupUser[]> {
    return this.http.get<GroupUsersResource[]>(`${groupsEndpointUrl}/${groupId}/members`).pipe(
      map(resources => this.assembler.toGroupUsersFromResources(resources)),
      catchError(this.handleError(`Failed to fetch members of group with id ${groupId}`))
    );
  }

  getGroupsByUser(): Observable<Group[]> {
    return this.http.get<GroupResource[]>(`${groupsEndpointUrl}/user`).pipe(
      map(resources => resources.map(resource => this.assembler.toEntityFromResource(resource))),
      catchError(this.handleError('Failed to fetch groups for the current user'))
    );
  }

  getGroupsByUserGroupRole(role: string): Observable<Group[]> {
    return this.http.get<GroupsResponse | GroupResource[]>(`${groupsEndpointUrl}/user/role?groupRole=${role}`).pipe(
      map(response => {
        if (Array.isArray(response))
          return response.map(resource => this.assembler.toEntityFromResource(resource));
        return this.assembler.toEntitiesFromResponse(response as GroupsResponse);
      }),
      catchError(this.handleError(`Failed to fetch groups for the current user with role ${role}`))
    );
  }

  /** Unirse a un grupo con su código (HUM-05). */
  joinByCode(code: string): Observable<Group> {
    return this.http.post<GroupResource>(`${groupsEndpointUrl}/join`, { code }).pipe(
      map(resource => this.assembler.toEntityFromResource(resource)),
      catchError(this.handleError('Failed to join group')),
    );
  }

  /** El líder quita a un integrante, o un integrante sale del grupo (HUM-05). */
  removeMember(groupId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${groupsEndpointUrl}/${groupId}/members/${userId}`).pipe(
      catchError(this.handleError('Failed to remove member')),
    );
  }
}
