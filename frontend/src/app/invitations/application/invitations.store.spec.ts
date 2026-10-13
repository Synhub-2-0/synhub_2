import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { InvitationsStore } from './invitations.store';
import { InvitationsApi } from '../infrastructure/invitations.api';
import { IamApi } from '../../iam/infrastructure/iam.api';
import { IamStore } from '../../iam/application/iam.store';
import { ToastStore } from '../../shared/application/toast.store';
import { Invitation } from '../domain/model/invitation.entity';
import { CreateInvitationCommand } from '../domain/model/create-invitation.command';
import { Group } from '../../groups/domain/model/group.entity';
import { Profile } from '../../iam/domain/model/profile.entity';

const profile = (id: number, username: string) => new Profile({ id, name: username, surname: 'T', email: '', imgUrl: '', userId: id, username });
const invitation = (id: number) => new Invitation({
  id,
  group: new Group({ id: 10, name: 'Equipo', imgUrl: '', description: '', code: 'ABC123XYZ', memberCount: 1 }),
  profile: profile(9, 'luis'),
});

describe('InvitationsStore', () => {
  let invitationsApi: Record<'createInvitation' | 'acceptInvitation' | 'declineInvitation' | 'getInvitationsByGroupId' | 'getInvitationsByAuthenticatedUser', ReturnType<typeof vi.fn>>;
  let iamApi: { getProfileByUsername: ReturnType<typeof vi.fn> };
  let store: InvitationsStore;
  let toasts: ToastStore;

  beforeEach(() => {
    localStorage.clear();
    invitationsApi = {
      createInvitation: vi.fn(), acceptInvitation: vi.fn(), declineInvitation: vi.fn(),
      getInvitationsByGroupId: vi.fn(), getInvitationsByAuthenticatedUser: vi.fn().mockReturnValue(of([])),
    };
    iamApi = { getProfileByUsername: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        { provide: InvitationsApi, useValue: invitationsApi },
        { provide: IamApi, useValue: iamApi },
        { provide: IamStore, useValue: { isSignedIn: signal(true), currentUsername: signal('ana') } },
      ],
    });
    store = TestBed.inject(InvitationsStore);
    toasts = TestBed.inject(ToastStore);
  });

  describe('inviteByUsername', () => {
    it('busca el perfil por username y crea la invitación con su id', () => {
      iamApi.getProfileByUsername.mockReturnValue(of(profile(9, 'luis')));
      invitationsApi.createInvitation.mockReturnValue(of(invitation(50)));
      const onSuccess = vi.fn();

      store.inviteByUsername('  luis ', 10, onSuccess);

      expect(iamApi.getProfileByUsername).toHaveBeenCalledWith('luis');
      const command = invitationsApi.createInvitation.mock.calls[0][0] as CreateInvitationCommand;
      expect([command.groupId, command.userId]).toEqual([10, 9]);
      expect(store.groupInvitations().map((i) => i.id)).toEqual([50]);
      expect(store.inviting()).toBe(false);
      expect(onSuccess).toHaveBeenCalled();
    });

    it('un username vacío no hace nada', () => {
      store.inviteByUsername('   ', 10);
      expect(iamApi.getProfileByUsername).not.toHaveBeenCalled();
    });

    it('no permite invitarse a sí mismo', () => {
      store.inviteByUsername('ana', 10);
      expect(iamApi.getProfileByUsername).not.toHaveBeenCalled();
      expect(store.error()).toBe('No puedes invitarte a ti mismo.');
    });

    it('usuario inexistente: mensaje claro y sin invitación', () => {
      iamApi.getProfileByUsername.mockReturnValue(throwError(() => new Error('Failed to fetch profile: Resource not found')));
      store.inviteByUsername('nadie', 10);
      expect(invitationsApi.createInvitation).not.toHaveBeenCalled();
      expect(store.error()).toBe('No existe un usuario con ese nombre.');
      expect(toasts.toasts()[0].message).toBe('No existe un usuario con ese nombre.');
    });

    it('el mensaje del backend (por ejemplo, ya es miembro) llega al usuario', () => {
      iamApi.getProfileByUsername.mockReturnValue(of(profile(9, 'luis')));
      invitationsApi.createInvitation.mockReturnValue(throwError(() => new Error('Este usuario ya es miembro del grupo.')));
      store.inviteByUsername('luis', 10);
      expect(store.error()).toBe('Este usuario ya es miembro del grupo.');
    });
  });

  it('carga mis invitaciones y las del grupo', () => {
    invitationsApi.getInvitationsByAuthenticatedUser.mockReturnValue(of([invitation(1), invitation(2)]));
    invitationsApi.getInvitationsByGroupId.mockReturnValue(of([invitation(3)]));
    store.loadInvitations();
    store.loadGroupInvitations(10);
    expect(store.invitationCount()).toBe(2);
    expect(store.groupInvitations().map((i) => i.id)).toEqual([3]);
    expect(invitationsApi.getInvitationsByGroupId).toHaveBeenCalledWith(10);
  });

  it('aceptar quita la invitación y llama a onSuccess', () => {
    invitationsApi.getInvitationsByAuthenticatedUser.mockReturnValue(of([invitation(1), invitation(2)]));
    invitationsApi.acceptInvitation.mockReturnValue(of(undefined));
    store.loadInvitations();
    const onSuccess = vi.fn();

    store.acceptInvitation(1, onSuccess);

    expect(store.invitations().map((i) => i.id)).toEqual([2]);
    expect(onSuccess).toHaveBeenCalled();
  });

  it('rechazar o cancelar quita la invitación de ambas listas', () => {
    invitationsApi.getInvitationsByAuthenticatedUser.mockReturnValue(of([invitation(1)]));
    invitationsApi.getInvitationsByGroupId.mockReturnValue(of([invitation(1)]));
    invitationsApi.declineInvitation.mockReturnValue(of(undefined));
    store.loadInvitations();
    store.loadGroupInvitations(10);

    store.declineInvitation(1);

    expect(store.invitations()).toEqual([]);
    expect(store.groupInvitations()).toEqual([]);
  });
});
