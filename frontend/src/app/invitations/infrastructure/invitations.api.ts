import {Injectable} from '@angular/core';
import {InvitationsApiEndpoint} from './invitations.api-endpoint';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Invitation} from '../domain/model/invitation.entity';
import {CreateInvitationCommand} from '../domain/model/create-invitation.command';

@Injectable({providedIn: 'root'})
export class InvitationsApi {
  private readonly invitationsEndpoint: InvitationsApiEndpoint;

  constructor(http: HttpClient) {
    this.invitationsEndpoint = new InvitationsApiEndpoint(http);
  }

  createInvitation(command: CreateInvitationCommand): Observable<Invitation> {
    return this.invitationsEndpoint.createInvitation(command);
  }

  acceptInvitation(id: number) {
    return this.invitationsEndpoint.acceptInvitation(id);
  }

  declineInvitation(id: number) {
    return this.invitationsEndpoint.declineInvitation(id);
  }

  getInvitationsByGroupId(groupId: number) {
    return this.invitationsEndpoint.getInvitationsByGroupId(groupId);
  }

  getInvitationsByAuthenticatedUser() {
    return this.invitationsEndpoint.getInvitationsByAuthenticatedUser();
  }
}
