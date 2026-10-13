import { SignInAssembler } from './sign-in/sign-in.assembler';
import { SignUpAssembler } from './sign-up/sign-up.assembler';
import { UsersAssembler } from './users/users.assembler';
import { ProfilesAssembler } from './profiles/profiles.assembler';
import { SignInCommand } from '../domain/model/sign-in.command';
import { SignUpCommand } from '../domain/model/sign-up.command';
import { ProfileResource } from './profiles/profiles.response';

describe('Assemblers de IAM', () => {
  it('SignIn: comando → request y respuesta → recurso', () => {
    const assembler = new SignInAssembler();
    expect(assembler.toRequestFromCommand(new SignInCommand({ username: 'ana', password: 'x' }))).toEqual({ username: 'ana', password: 'x' });
    expect(assembler.toResourceFromResponse({ id: 5, username: 'ana', role: ['ROLE_USER'], expiresAt: '2099-01-01T00:00:00Z' }))
      .toEqual({ id: 5, username: 'ana', role: ['ROLE_USER'], expiresAt: '2099-01-01T00:00:00Z' });
  });

  it('SignUp: envía los campos del formulario y nunca roles (AUDIT SEC-05)', () => {
    const command = new SignUpCommand({
      username: 'ana', password: 'Secret123!', name: 'Ana', surname: 'Torres', email: 'ana@synhub.io', imgUrl: 'https://img',
    });
    expect(new SignUpAssembler().toRequestFromCommand(command)).toEqual({
      username: 'ana', name: 'Ana', surname: 'Torres', imgUrl: 'https://img', email: 'ana@synhub.io', password: 'Secret123!',
    });
  });

  it('Users: resource ↔ entidad y respuesta envoltorio', () => {
    const assembler = new UsersAssembler();
    const user = assembler.toEntityFromResource({ id: 5, username: 'ana', accountRoles: ['ROLE_USER'] });
    expect([user.id, user.username, user.accountRoles]).toEqual([5, 'ana', ['ROLE_USER']]);
    expect(assembler.toResourceFromEntity(user)).toEqual({ id: 5, username: 'ana', accountRoles: ['ROLE_USER'] });
    expect(assembler.toEntitiesFromResponse({ users: [{ id: 1, username: 'a', accountRoles: [] }] })).toHaveLength(1);
  });

  it('Profiles: resource ↔ entidad', () => {
    const assembler = new ProfilesAssembler();
    const resource: ProfileResource = { id: 5, name: 'Ana', surname: 'Torres', email: 'ana@synhub.io', imgUrl: '', username: 'ana' };
    const profile = assembler.toEntityFromResource(resource);
    expect([profile.id, profile.name, profile.surname, profile.username]).toEqual([5, 'Ana', 'Torres', 'ana']);
    expect(assembler.toResourceFromEntity(profile)).toEqual(resource);
  });

  it('AUDIT BUG-15: un perfil armado desde UserResource (sin userId) no deja userId undefined', () => {
    const fromBackend = { id: 5, name: 'Ana', surname: 'Torres', email: 'ana@synhub.io', imgUrl: '', username: 'ana' } as ProfileResource;
    expect(new ProfilesAssembler().toEntityFromResource(fromBackend).userId).toBe(5);
  });
});
