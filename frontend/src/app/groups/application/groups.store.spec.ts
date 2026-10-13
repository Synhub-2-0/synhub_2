import { TestBed } from '@angular/core/testing';
import { Location } from '@angular/common';
import { signal } from '@angular/core';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { GroupsStore } from './groups.store';
import { GroupsApi } from '../infrastructure/groups.api';
import { IamStore } from '../../iam/application/iam.store';
import { ToastStore } from '../../shared/application/toast.store';
import { Group } from '../domain/model/group.entity';
import { GroupUser } from '../domain/model/group-user.entity';
import { CreateGroupCommand } from '../domain/model/create-group.command';
import { Profile } from '../../iam/domain/model/profile.entity';

const group = (id: number, name = `Grupo ${id}`) =>
  new Group({ id, name, imgUrl: '', description: 'd', code: 'ABC123XYZ', memberCount: 1 });

describe('GroupsStore', () => {
  let api: Record<'getLeaderGroups' | 'getMemberGroups' | 'getGroupMembers' | 'createGroup' | 'updateGroup' | 'joinGroupByCode' | 'removeMember', ReturnType<typeof vi.fn>>;
  let isSignedIn: ReturnType<typeof signal<boolean>>;
  let router: { navigate: ReturnType<typeof vi.fn> };
  let store: GroupsStore;
  let toasts: ToastStore;

  beforeEach(() => {
    localStorage.clear();
    // Con sesión iniciada, el store carga los grupos al crearse.
    api = { getLeaderGroups: vi.fn().mockReturnValue(of([])), getMemberGroups: vi.fn().mockReturnValue(of([])), getGroupMembers: vi.fn(), createGroup: vi.fn(), updateGroup: vi.fn(), joinGroupByCode: vi.fn(), removeMember: vi.fn() };
    isSignedIn = signal(true);
    router = { navigate: vi.fn().mockResolvedValue(true) };
    TestBed.configureTestingModule({
      providers: [
        { provide: GroupsApi, useValue: api },
        { provide: IamStore, useValue: { isSignedIn, currentUserId: signal(7) } },
        { provide: Location, useValue: {} },
      ],
    });
    store = TestBed.inject(GroupsStore);
    toasts = TestBed.inject(ToastStore);
  });

  it('loadGroups separa líder y miembro y une sin duplicar', () => {
    api.getLeaderGroups.mockReturnValue(of([group(1), group(2)]));
    api.getMemberGroups.mockReturnValue(of([group(2), group(3)]));

    store.loadGroups();

    expect(store.leaderGroups().map((g) => g.id)).toEqual([1, 2]);
    expect(store.memberGroups().map((g) => g.id)).toEqual([2, 3]);
    expect(store.groups().map((g) => g.id)).toEqual([1, 2, 3]);
    expect(store.groupCount()).toBe(3);
    expect(store.loading()).toBe(false);
    expect(store.getGroupById(3)()?.name).toBe('Grupo 3');
    expect(store.getGroupById(99)()).toBeUndefined();
  });

  it('un error al cargar grupos queda en error y en un toast', () => {
    api.getLeaderGroups.mockReturnValue(throwError(() => new Error('Failed to fetch groups: 500')));
    api.getMemberGroups.mockReturnValue(of([]));
    store.loadGroups();
    expect(store.error()).toBe('Failed to fetch groups: 500');
    expect(toasts.toasts()[0].message).toBe('El servidor tuvo un problema. Inténtalo más tarde.');
    expect(store.loading()).toBe(false);
  });

  it('loadGroupMembers guarda los integrantes por grupo; si falla deja la lista vacía', () => {
    const member = new GroupUser({ user: new Profile({ id: 2, name: 'Ana', surname: 'T', email: '', imgUrl: '', userId: 2 }), roleInGroup: 'GROUP_MEMBER' });
    api.getGroupMembers.mockReturnValueOnce(of([member])).mockReturnValueOnce(throwError(() => new Error('Failed: 403')));

    store.loadGroupMembers(1);
    store.loadGroupMembers(2);

    expect(store.getMembersByGroupId(1)()).toEqual([member]);
    expect(store.getMembersByGroupId(2)()).toEqual([]);
    expect(store.membersLoading()).toBe(false);
  });

  it('addGroup lo agrega como liderado, recarga y navega', () => {
    api.createGroup.mockReturnValue(of(group(5, 'Nuevo')));
    api.getLeaderGroups.mockReturnValue(of([group(5, 'Nuevo')]));
    api.getMemberGroups.mockReturnValue(of([]));

    store.addGroup(new CreateGroupCommand({ name: 'Nuevo', description: 'd', imgUrl: '' }), router as unknown as Router);

    expect(store.leaderGroups().map((g) => g.name)).toEqual(['Nuevo']);
    expect(api.getLeaderGroups).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/groups/leader']);
  });

  it('updateGroup reemplaza el grupo en las listas', () => {
    api.getLeaderGroups.mockReturnValue(of([group(1)]));
    api.getMemberGroups.mockReturnValue(of([]));
    store.loadGroups();
    api.updateGroup.mockReturnValue(of(group(1, 'Renombrado')));

    store.updateGroup(group(1, 'Renombrado'), router as unknown as Router);

    expect(store.groups()[0].name).toBe('Renombrado');
    expect(store.leaderGroups()[0].name).toBe('Renombrado');
  });

  it('al cerrar sesión se borran los datos del usuario anterior', () => {
    api.getLeaderGroups.mockReturnValue(of([group(1)]));
    api.getMemberGroups.mockReturnValue(of([]));
    store.loadGroups();
    isSignedIn.set(false);
    TestBed.tick();
    expect(store.groups()).toEqual([]);
    expect(store.leaderGroups()).toEqual([]);
  });

  describe('HUM-05: unirse por código y quitar integrantes', () => {
    const member = (id: number, role = 'GROUP_MEMBER') => new GroupUser({
      user: new Profile({ id, name: `U${id}`, surname: 'T', email: '', imgUrl: '', userId: id, username: `u${id}` }), roleInGroup: role,
    });

    it('normaliza el código, se une, avisa y recarga los grupos', () => {
      api.joinGroupByCode.mockReturnValue(of(group(4, 'Equipo Rojo')));
      const done = vi.fn();
      store.joinGroupByCode('  abc123xyz ', done);
      expect(api.joinGroupByCode).toHaveBeenCalledWith('ABC123XYZ');
      expect(toasts.toasts().at(-1)?.message).toBe('Te uniste a Equipo Rojo.');
      expect(api.getLeaderGroups).toHaveBeenCalledTimes(2);
      expect(done).toHaveBeenCalled();
      expect(store.joining()).toBe(false);
    });

    it('un código mal formado no llama a la API', () => {
      store.joinGroupByCode('corto');
      expect(api.joinGroupByCode).not.toHaveBeenCalled();
      expect(toasts.toasts()[0].message).toBe('El código tiene 9 caracteres: letras y números.');
    });

    it('traduce los errores del backend', () => {
      api.joinGroupByCode.mockReturnValue(throwError(() => new Error('Failed to join group: Resource not found')));
      store.joinGroupByCode('ZZZZZZZZZ');
      api.joinGroupByCode.mockReturnValue(throwError(() => new Error('Failed to join group: 409')));
      store.joinGroupByCode('ABC123XYZ');
      expect(toasts.toasts().map((t) => t.message)).toEqual(['No existe un grupo con ese código.', 'Ya perteneces a este grupo.']);
    });

    it('quitar a un integrante lo saca de la lista en caché', () => {
      api.getGroupMembers.mockReturnValue(of([member(1, 'GROUP_LEADER'), member(2), member(3)]));
      store.loadGroupMembers(10);
      api.removeMember.mockReturnValue(of(undefined));

      store.removeMember(10, 2);

      expect(api.removeMember).toHaveBeenCalledWith(10, 2);
      expect(store.getMembersByGroupId(10)().map((m) => m.user.id)).toEqual([1, 3]);
    });

    it('quitar al líder muestra la regla de negocio', () => {
      api.removeMember.mockReturnValue(throwError(() => new Error('Failed to remove member: 409')));
      store.removeMember(10, 1);
      expect(toasts.toasts()[0].message).toBe('El líder no puede salir del grupo.');
    });

    it('salir del grupo usa el usuario actual y vuelve a Mis grupos', () => {
      api.removeMember.mockReturnValue(of(undefined));
      store.leaveGroup(10, router as unknown as Router);
      expect(api.removeMember).toHaveBeenCalledWith(10, 7);
      expect(router.navigate).toHaveBeenCalledWith(['/groups/member']);
    });
  });
});
