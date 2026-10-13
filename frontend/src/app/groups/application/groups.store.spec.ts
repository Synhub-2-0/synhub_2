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
  let api: Record<'getLeaderGroups' | 'getMemberGroups' | 'getGroupMembers' | 'createGroup' | 'updateGroup', ReturnType<typeof vi.fn>>;
  let isSignedIn: ReturnType<typeof signal<boolean>>;
  let router: { navigate: ReturnType<typeof vi.fn> };
  let store: GroupsStore;
  let toasts: ToastStore;

  beforeEach(() => {
    localStorage.clear();
    // Con sesión iniciada, el store carga los grupos al crearse.
    api = { getLeaderGroups: vi.fn().mockReturnValue(of([])), getMemberGroups: vi.fn().mockReturnValue(of([])), getGroupMembers: vi.fn(), createGroup: vi.fn(), updateGroup: vi.fn() };
    isSignedIn = signal(true);
    router = { navigate: vi.fn().mockResolvedValue(true) };
    TestBed.configureTestingModule({
      providers: [
        { provide: GroupsApi, useValue: api },
        { provide: IamStore, useValue: { isSignedIn } },
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
});
