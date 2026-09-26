import {environment} from '../../../environments/environment';
import {BaseApiEndpoint} from '../../shared/infrastructure/base-api-endpoint';
import {Invitation} from '../domain/model/invitation.entity';
import {CreateInvitationRequest, InvitationResource, InvitationsResponse} from './invitations.response';
import {InvitationsAssembler} from './invitations.assembler';
import {HttpClient} from '@angular/common/http';
import {catchError, map, Observable} from 'rxjs';

const invitationsEndpointUrl = `${environment.platformProviderApiBaseUrl}${environment.platformProviderInvitationsEndpointPath}`;

export class InvitationsApiEndpoint extends BaseApiEndpoint<Invitation, InvitationResource, InvitationsResponse, InvitationsAssembler> {
  constructor(http: HttpClient) {
    super(http, invitationsEndpointUrl, new InvitationsAssembler());
  }

  override create(entity: Invitation): Observable<Invitation> {
    const request: CreateInvitationRequest = {
      groupId: entity.group.id,
      userId: entity.profile.id
    };
    return this.http.post<InvitationResource>(this.endpointUrl, request).pipe(
      map(resource => this.assembler.toEntityFromResource(resource)),
      catchError(this.handleError('Failed to create invitation'))
    );
  }

  acceptInvitation(invitationId: number): Observable<void> {
    return this.http.post<void>(`${this.endpointUrl}/accept?invitationId=${invitationId}`, {}).pipe(
      catchError(this.handleError(`Failed to accept invitation with id ${invitationId}`))
    );
  }

  getInvitationsByGroupId(groupId: number): Observable<Invitation[]> {
    return this.http.get<InvitationResource[]>(`${this.endpointUrl}/group?groupId=${groupId}`).pipe(
      map(resources => resources.map(resource => this.assembler.toEntityFromResource(resource))),
      catchError(this.handleError(`Failed to fetch invitations for group with ID ${groupId}`))
    );
  }

  getInvitationsByAuthenticatedUser(): Observable<Invitation[]> {
    return this.http.get<InvitationResource[]>(`${this.endpointUrl}/user`).pipe(
      map(resources => resources.map(resource => this.assembler.toEntityFromResource(resource))),
      catchError(this.handleError('Failed to fetch invitations for authenticated user'))
    );
  }

  declineInvitation(invitationId: number): Observable<void> {
    return this.http.delete<void>(`${this.endpointUrl}/${invitationId}`).pipe(
      catchError(this.handleError(`Failed to decline invitation with ID ${invitationId}`))
    );
  }
}
