import { GroupsAssembler } from './groups.assembler';
import { GroupResource } from './groups.response';
import { CreateGroupCommand } from '../domain/model/create-group.command';

describe('GroupsAssembler', () => {
  const assembler = new GroupsAssembler();
  const resource: GroupResource = { id: 1, name: 'Equipo', imgUrl: 'https://img', description: 'd', code: 'ABC123XYZ', memberCount: 4 };

  it('resource ↔ entidad', () => {
    const group = assembler.toEntityFromResource(resource);
    expect([group.id, group.name, group.imgUrl, group.code, group.memberCount]).toEqual([1, 'Equipo', 'https://img', 'ABC123XYZ', 4]);
    expect(assembler.toResourceFromEntity(group)).toEqual(resource);
  });

  it('memberCount ausente vale 0', () => {
    const { memberCount, ...withoutCount } = resource;
    expect(assembler.toEntityFromResource(withoutCount as GroupResource).memberCount).toBe(0);
  });

  it('respuesta envoltorio, incluso vacía', () => {
    expect(assembler.toEntitiesFromResponse({ groups: [resource] })).toHaveLength(1);
    expect(assembler.toEntitiesFromResponse(undefined as never)).toEqual([]);
  });

  it('integrantes con su perfil y rol', () => {
    const [member] = assembler.toGroupUsersFromResources([
      { user: { id: 2, name: 'Ana', surname: 'Torres', email: 'a@x', imgUrl: '', userId: 2, username: 'ana' }, roleInGroup: 'GROUP_LEADER' },
    ]);
    expect(member.user.username).toBe('ana');
    expect(member.roleInGroup).toBe('GROUP_LEADER');
    expect(assembler.toGroupUsersFromResources(undefined as never)).toEqual([]);
  });

  it('comando de creación → request', () => {
    expect(assembler.toRequestFromCreateCommand(new CreateGroupCommand({ name: 'N', description: 'D', imgUrl: 'I' })))
      .toEqual({ name: 'N', description: 'D', imgUrl: 'I' });
  });
});
