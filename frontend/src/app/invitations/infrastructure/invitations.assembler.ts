import {BaseAssembler} from '../../shared/infrastructure/base-assembler';
import {Invitation} from '../domain/model/invitation.entity';
import {CreateInvitationRequest, InvitationResource, InvitationsResponse} from './invitations.response';
import {CreateInvitationCommand} from '../domain/model/create-invitation.command';
import {GroupsAssembler} from '../../groups/infrastructure/groups.assembler';
import {ProfilesAssembler} from '../../iam/infrastructure/profiles/profiles.assembler';

export class InvitationsAssembler implements BaseAssembler<Invitation, InvitationResource, InvitationsResponse> {
  private groupAssembler = new GroupsAssembler();
  private profileAssembler = new ProfilesAssembler();

  toEntitiesFromResponse(response: InvitationsResponse): Invitation[] {
    return response.invitations.map(resource => this.toEntityFromResource(resource as InvitationResource))
  }

  toEntityFromResource(resource: InvitationResource): Invitation {
    return new Invitation({
      id: resource.id,
      group: this.groupAssembler.toEntityFromResource(resource.group),
      profile: this.profileAssembler.toEntityFromResource(resource.user)
    })
  }

  toResourceFromEntity(entity: Invitation): InvitationResource {
    return {
      id: entity.id,
      group: this.groupAssembler.toResourceFromEntity(entity.group),
      user: this.profileAssembler.toResourceFromEntity(entity.profile)
    } as InvitationResource;
  }

  toRequestFromCreateCommand(command: CreateInvitationCommand): CreateInvitationRequest {
    return {
      groupId: command.groupId,
      userId: command.userId
    } as CreateInvitationRequest;
  }
}
