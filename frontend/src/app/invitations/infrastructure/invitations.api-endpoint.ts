import {environment} from '../../../environments/environment';
import {BaseApiEndpoint} from '../../shared/infrastructure/base-api-endpoint';
import {Invitation} from '../domain/model/invitation.entity';
import {InvitationResource, InvitationsResponse} from './invitations.response';
import {CreateInvitationCommand} from '../domain/model/create-invitation.command';
import {InvitationsAssembler} from './invitations.assembler';
import {HttpClient, HttpErrorResponse} from '@angular/common/http';
import {catchError, map, Observable, throwError} from 'rxjs';

const invitationsEndpointUrl = `${environment.platformProviderApiBaseUrl}${environment.platformProviderInvitationsEndpointPath}`;

export class InvitationsApiEndpoint extends BaseApiEndpoint<Invitation, InvitationResource, InvitationsResponse, InvitationsAssembler> {
  constructor(http: HttpClient) {
    super(http, invitationsEndpointUrl, new InvitationsAssembler());
  }

  createInvitation(command: CreateInvitationCommand): Observable<Invitation> {
    const request = this.assembler.toRequestFromCreateCommand(command);
    return this.http.post<InvitationResource>(this.endpointUrl, request).pipe(
      map(resource => this.assembler.toEntityFromResource(resource)),
      catchError((error: HttpErrorResponse) => throwError(() => new Error(this.createInvitationErrorMessage(error))))
    );
  }

  /*
   * TODO: Reactivar cuando el backend permita solicitar la unión a un grupo.
   *
   * inviteUserToGroup(groupId: number, userId: number): Observable<Invitation> {
   *   return this.http.post<InvitationResource>(this.endpointUrl, { groupId, userId }).pipe(
   *     map(resource => this.assembler.toEntityFromResource(resource)),
   *     catchError(this.handleError('Failed to request group invitation')),
   *   );
   * }
   */

  acceptInvitation(invitationId: number): Observable<void> {
    return this.http.post<void>(`${this.endpointUrl}/accept?invitationId=${invitationId}`, {}).pipe(
      catchError(this.handleError(`Failed to accept invitation with id ${invitationId}`))
    );
  }

  getInvitationsByGroupId(groupId: number): Observable<Invitation[]> {
    return this.http.get<InvitationsResponse | InvitationResource[]>(`${this.endpointUrl}/group?groupId=${groupId}`).pipe(
      map(response => {
        if (Array.isArray(response))
          return response.map(resource => this.assembler.toEntityFromResource(resource));
        return this.assembler.toEntitiesFromResponse(response as InvitationsResponse);
      }),
      catchError(this.handleError(`Failed to fetch invitations for group with ID ${groupId}`))
    );
  }

  getInvitationsByAuthenticatedUser(): Observable<Invitation[]> {
    return this.http.get<InvitationsResponse | InvitationResource[]>(`${this.endpointUrl}/user`).pipe(
      map(response => {
        if (Array.isArray(response))
          return response.map(resource => this.assembler.toEntityFromResource(resource));
        return this.assembler.toEntitiesFromResponse(response as InvitationsResponse);
      }),
      catchError(this.handleError('Failed to fetch invitations for authenticated user'))
    );
  }

  declineInvitation(invitationId: number): Observable<void> {
    return this.http.delete<void>(`${this.endpointUrl}/${invitationId}`).pipe(
      catchError(this.handleError(`Failed to decline invitation with ID ${invitationId}`))
    );
  }

  // The backend answers in English with ids, so the status (and the 409 message) is mapped to a user-facing text.
  private createInvitationErrorMessage(error: HttpErrorResponse): string {
    const backendMessage: string = error.error?.message ?? '';
    switch (error.status) {
      case 403: return 'Solo el líder del grupo puede enviar invitaciones.';
      case 404: return 'No se encontró el usuario o el grupo.';
      case 409: return backendMessage.includes('is already in group')
        ? 'Este usuario ya es miembro del grupo.'
        : 'Este usuario ya tiene una invitación pendiente.';
      default: return 'No se pudo enviar la invitación.';
    }
  }
}
