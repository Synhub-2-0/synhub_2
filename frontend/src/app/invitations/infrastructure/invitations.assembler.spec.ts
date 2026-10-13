import { InvitationsAssembler } from './invitations.assembler';
import { InvitationResource } from './invitations.response';
import { CreateInvitationCommand } from '../domain/model/create-invitation.command';

describe('InvitationsAssembler', () => {
  const assembler = new InvitationsAssembler();
  const resource: InvitationResource = {
    id: 50,
    group: { id: 10, name: 'Equipo', imgUrl: '', description: 'd', code: 'ABC123XYZ', memberCount: 3 },
    user: { id: 9, name: 'Luis', surname: 'Paz', email: 'l@x', imgUrl: '', username: 'luis' },
  };

  it('resource → entidad: el "user" del backend pasa a "profile"', () => {
    const invitation = assembler.toEntityFromResource(resource);
    expect(invitation.id).toBe(50);
    expect(invitation.group.name).toBe('Equipo');
    expect(invitation.profile.username).toBe('luis');
  });

  it('ida y vuelta conserva los datos', () => {
    expect(assembler.toResourceFromEntity(assembler.toEntityFromResource(resource))).toEqual(resource);
  });

  it('respuesta envoltorio y comando → request', () => {
    expect(assembler.toEntitiesFromResponse({ invitations: [resource] })).toHaveLength(1);
    expect(assembler.toRequestFromCreateCommand(new CreateInvitationCommand({ groupId: 10, userId: 9 }))).toEqual({ groupId: 10, userId: 9 });
  });
});
