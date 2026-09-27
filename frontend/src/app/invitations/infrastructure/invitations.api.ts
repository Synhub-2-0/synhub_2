import {Injectable} from '@angular/core';
import {BaseApi} from '../../shared/infrastructure/base-api';
import {InvitationsApiEndpoint} from './invitations.api-endpoint';
import {HttpClient} from '@angular/common/http';

@Injectable({providedIn: 'root'})
export class InvitationsApi extends BaseApi {
  private readonly invitationsEndpoint: InvitationsApiEndpoint;

  constructor(http: HttpClient) {
    super();
    this.invitationsEndpoint = new InvitationsApiEndpoint(http);
  }

  createInvitation(invitation: any) {
    return this.invitationsEndpoint.create(invitation);
  }

  /*
   * TODO: Reactivar cuando el backend permita solicitar la unión a un grupo.
   *
   * inviteUserToGroup(groupId: number, userId: number) {
   *   return this.invitationsEndpoint.inviteUserToGroup(groupId, userId);
   * }
   */

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
