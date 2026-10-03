import {BaseApiEndpoint} from '../../shared/infrastructure/base-api-endpoint';
import {Group} from '../domain/model/group.entity';
import {GroupResource, GroupsResponse} from './groups.response';
import {GroupsAssembler} from './groups.assembler';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';
import {catchError, map, Observable} from 'rxjs';
import {CreateGroupCommand} from '../domain/model/create-group.command';

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

  /*
   * TODO: Reactivar cuando el backend exponga este endpoint.
   *
   * searchByCode(code: string): Observable<Group> {
   *   return this.http.get<GroupResource>(`${groupsEndpointUrl}/search?code=${encodeURIComponent(code)}`).pipe(
   *     map(resource => this.assembler.toEntityFromResource(resource)),
   *     catchError(this.handleError('Failed to search group by code')),
   *   );
   * }
   */
}
