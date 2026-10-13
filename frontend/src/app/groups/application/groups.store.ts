import { ToastStore } from '../../shared/application/toast.store';
import { computed, Injectable, Signal, signal, inject, effect } from '@angular/core';
import { Router } from '@angular/router';
import { Location } from '@angular/common';
import { forkJoin, retry } from 'rxjs';
import { Group } from '../domain/model/group.entity';
import { GroupUser } from '../domain/model/group-user.entity';
import { GroupsApi } from '../infrastructure/groups.api';
import { CreateGroupCommand } from '../domain/model/create-group.command';
import { IamStore } from '../../iam/application/iam.store';

@Injectable({ providedIn: 'root' })
export class GroupsStore {
  private groupsApi = inject(GroupsApi);
  private iamStore = inject(IamStore);
  private location = inject(Location);

  private readonly toastStore = inject(ToastStore);
  private readonly groupsSignal = signal<Group[]>([]);
  private readonly leaderGroupsSignal = signal<Group[]>([]);
  private readonly memberGroupsSignal = signal<Group[]>([]);
  private readonly membersByGroupSignal = signal<Record<number, GroupUser[]>>({});
  private readonly loadingSignal = signal<boolean>(false);
  private readonly membersLoadingSignal = signal<boolean>(false);
  private readonly joiningSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly groups = this.groupsSignal.asReadonly();
  readonly leaderGroups = this.leaderGroupsSignal.asReadonly();
  readonly memberGroups = this.memberGroupsSignal.asReadonly();
  readonly membersByGroup = this.membersByGroupSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly membersLoading = this.membersLoadingSignal.asReadonly();
  readonly joining = this.joiningSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly groupCount = computed(() => this.groups().length);

  constructor() {
    // Root stores outlive the session: drop the previous user's data as soon as the session ends.
    effect(() => {
      if (!this.iamStore.isSignedIn()) this.reset();
    });
    if (this.iamStore.isSignedIn()) {
      this.loadGroups();
    }
  }

  reset(): void {
    this.groupsSignal.set([]);
    this.leaderGroupsSignal.set([]);
    this.memberGroupsSignal.set([]);
    this.membersByGroupSignal.set({});
    this.errorSignal.set(null);
  }

  getGroupById(id: number): Signal<Group | undefined> {
    return computed(() => (id ? this.groups().find((g) => g.id === id) : undefined));
  }

  loadGroups(): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    // The group payload no longer carries its members, so the role of the user is resolved by the role endpoints.
    forkJoin({
      leader: this.groupsApi.getLeaderGroups(),
      member: this.groupsApi.getMemberGroups(),
    }).subscribe({
      next: ({ leader, member }) => {
        const allGroups = [...leader, ...member.filter((m) => !leader.some((l) => l.id === m.id))];
        this.leaderGroupsSignal.set(leader);
        this.memberGroupsSignal.set(member);
        this.groupsSignal.set(allGroups);
        this.loadingSignal.set(false);
      },
      error: (err) => {
        this.setError(this.formatError(err, 'Failed to load groups'));
        this.loadingSignal.set(false);
      },
    });
  }

  getMembersByGroupId(groupId: number): Signal<GroupUser[]> {
    return computed(() => this.membersByGroup()[groupId] ?? []);
  }

  loadGroupMembers(groupId: number): void {
    this.membersLoadingSignal.set(true);
    this.groupsApi.getGroupMembers(groupId).subscribe({
      next: (members) => {
        this.membersByGroupSignal.update((cache) => ({ ...cache, [groupId]: members }));
        this.membersLoadingSignal.set(false);
      },
      error: (err) => {
        this.membersByGroupSignal.update((cache) => ({ ...cache, [groupId]: [] }));
        this.setError(this.formatError(err, 'Failed to load group members'));
        this.membersLoadingSignal.set(false);
      },
    });
  }

  addGroup(createGroupCommand: CreateGroupCommand, router: Router): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.groupsApi
      .createGroup(createGroupCommand)
      .pipe(retry(2))
      .subscribe({
        next: (createdGroup) => {
          this.groupsSignal.update((groups) => [...groups, createdGroup]);
          this.leaderGroupsSignal.update((groups) => [...groups, createdGroup]);
          this.loadingSignal.set(false);
          this.loadGroups(); // Sincroniza con la base de datos
          router.navigate(['/groups/leader']).then();
        },
        error: (err) => {
          this.setError(this.formatError(err, 'Failed to create group'));
          this.loadingSignal.set(false);
        },
      });
  }

  updateGroup(updatedGroup: Group, router: Router): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.groupsApi
      .updateGroup(updatedGroup)
      .pipe(retry(2))
      .subscribe({
        next: (group) => {
          this.groupsSignal.update((groups) => groups.map((g) => (g.id === group.id ? group : g)));
          this.leaderGroupsSignal.update((groups) =>
            groups.map((g) => (g.id === group.id ? group : g)),
          );
          this.loadingSignal.set(false);
          router.navigate(['/groups/leader']).then();
        },
        error: (err) => {
          this.setError(this.formatError(err, 'Failed to update group'));
          this.loadingSignal.set(false);
        },
      });
  }

  /** Unirse con el código de 9 caracteres del grupo (HUM-05). */
  joinGroupByCode(code: string, onSuccess?: () => void): void {
    const normalized = code.trim().toUpperCase();
    if (!/^[0-9A-Z]{9}$/.test(normalized)) {
      this.setError('El código tiene 9 caracteres: letras y números.');
      return;
    }
    this.joiningSignal.set(true);
    this.errorSignal.set(null);
    this.groupsApi.joinGroupByCode(normalized).subscribe({
      next: (group) => {
        this.joiningSignal.set(false);
        this.toastStore.success(`Te uniste a ${group.name}.`);
        this.loadGroups();
        onSuccess?.();
      },
      error: (err) => {
        this.joiningSignal.set(false);
        this.setError(
          this.membershipErrorMessage(
            err,
            {
              404: 'No existe un grupo con ese código.',
              409: 'Ya perteneces a este grupo.',
              400: 'El código no es válido.',
            },
            'No se pudo unir al grupo. Inténtalo nuevamente.',
          ),
        );
      },
    });
  }

  /** El líder quita a un integrante; sus tareas se borran con él (HUM-05). */
  removeMember(groupId: number, userId: number, onSuccess?: () => void): void {
    this.groupsApi.removeMember(groupId, userId).subscribe({
      next: () => {
        this.membersByGroupSignal.update((cache) => ({
          ...cache,
          [groupId]: (cache[groupId] ?? []).filter((member) => member.user.id !== userId),
        }));
        this.toastStore.success('Integrante quitado del grupo.');
        this.loadGroups();
        onSuccess?.();
      },
      error: (err) =>
        this.setError(
          this.membershipErrorMessage(
            err,
            {
              403: 'Solo el líder puede quitar integrantes.',
              404: 'Esa persona ya no pertenece al grupo.',
              409: 'El líder no puede salir del grupo.',
            },
            'No se pudo quitar al integrante. Inténtalo nuevamente.',
          ),
        ),
    });
  }

  /** El usuario actual sale del grupo y vuelve a "Mis grupos". */
  leaveGroup(groupId: number, router: Router): void {
    const userId = this.iamStore.currentUserId();
    if (!userId) return;
    this.groupsApi.removeMember(groupId, userId).subscribe({
      next: () => {
        this.toastStore.success('Saliste del grupo.');
        this.loadGroups();
        router.navigate(['/groups/member']).then();
      },
      error: (err) =>
        this.setError(
          this.membershipErrorMessage(
            err,
            {
              409: 'El líder no puede salir del grupo.',
            },
            'No se pudo salir del grupo. Inténtalo nuevamente.',
          ),
        ),
    });
  }

  private membershipErrorMessage(
    error: unknown,
    byStatus: Record<number, string>,
    fallback: string,
  ): string {
    const message = error instanceof Error ? error.message : '';
    const status = message.includes('Resource not found')
      ? 404
      : Number(/:\s*(\d{3})\s*$/.exec(message)?.[1]);
    return byStatus[status] ?? fallback;
  }

  private formatError(error: unknown, fallback: string): string {
    if (error instanceof Error) {
      return error.message.includes('Resource not found')
        ? `${fallback}: Not found`
        : error.message;
    }
    return fallback;
  }

  private setError(message: string): void {
    this.errorSignal.set(message);
    this.toastStore.error(message);
  }
}
