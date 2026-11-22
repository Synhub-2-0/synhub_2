import {BaseApiEndpoint} from '../../shared/infrastructure/base-api-endpoint';
import {Group} from '../domain/model/group.entity';
import {GroupResource, GroupsResponse, GroupUsersResource} from './groups.response';
import {GroupsAssembler} from './groups.assembler';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {environment} from '../../../environments/environment';
import {catchError, map, Observable, throwError} from 'rxjs';
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

  /** El líder elimina su grupo (también sus tareas, solicitudes e invitaciones); libera un cupo del plan. */
  deleteGroup(groupId: number): Observable<void> {
    return this.http.delete<void>(`${groupsEndpointUrl}/${groupId}`).pipe(
      catchError((error: HttpErrorResponse) => throwError(() => new Error(this.deleteErrorMessage(error)))),
    );
  }

  private deleteErrorMessage(error: HttpErrorResponse): string {
    switch (error.status) {
      case 403: return 'Solo el líder del grupo puede eliminarlo.';
      case 404: return 'El grupo ya no existe.';
      default: return 'No se pudo eliminar el grupo.';
    }
  }

  /** Activar un grupo bloqueado (solo su líder y con un cupo libre en su plan). */
  activateGroup(groupId: number): Observable<Group> {
    return this.http.post<GroupResource>(`${groupsEndpointUrl}/${groupId}/activate`, null).pipe(
      map(resource => this.assembler.toEntityFromResource(resource)),
      catchError((error: HttpErrorResponse) => throwError(() => new Error(this.activateErrorMessage(error)))),
    );
  }

  // The backend answers in English: the status is mapped to a user-facing text.
  private activateErrorMessage(error: HttpErrorResponse): string {
    switch (error.status) {
      case 403: return 'Solo el líder del grupo puede activarlo.';
      case 409: return 'No tienes cupo para activar otro grupo: elimina uno de tus grupos activos o sube de plan.';
      default: return 'No se pudo activar el grupo.';
    }
  }

  /** El líder quita a un integrante, o un integrante sale del grupo (HUM-05). */
  removeMember(groupId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${groupsEndpointUrl}/${groupId}/members/${userId}`).pipe(
      catchError(this.handleError('Failed to remove member')),
    );
  }
}
