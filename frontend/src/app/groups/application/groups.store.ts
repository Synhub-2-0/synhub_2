import { computed, Injectable, Signal, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Location } from '@angular/common';
import { forkJoin, of, catchError, retry, switchMap, map } from 'rxjs';
import { Group } from '../domain/model/group.entity';
import { GroupsApi } from '../infrastructure/groups.api';
import { CreateGroupCommand } from '../domain/model/create-group.command';
import { IamStore } from '../../iam/application/iam.store';

@Injectable({ providedIn: 'root' })
export class GroupsStore {
  private readonly groupsSignal = signal<Group[]>([]);
  private readonly leaderGroupsSignal = signal<Group[]>([]);
  private readonly memberGroupsSignal = signal<Group[]>([]);
  private readonly loadingSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly groups = this.groupsSignal.asReadonly();
  readonly leaderGroups = this.leaderGroupsSignal.asReadonly();
  readonly memberGroups = this.memberGroupsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly groupCount = computed(() => this.groups().length);

  constructor(
    private groupsApi: GroupsApi,
    private iamStore: IamStore,
    private location: Location,
  ) {
    if (localStorage.getItem('token')) {
      this.loadGroups();
    }
  }

  getGroupById(id: number): Signal<Group | undefined> {
    return computed(() => (id ? this.groups().find((g) => g.id === id) : undefined));
  }

  loadGroups(): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    // Consultamos los grupos del usuario actual de manera dinámica
    this.groupsApi
      .getGroupsByUser()
      .pipe(
        switchMap((userGroups) => {
          if (!userGroups || userGroups.length === 0) {
            return of([] as Group[]);
          }
          // Obtenemos el detalle completo de cada grupo devuelto por el backend
          return forkJoin(
            userGroups.map((g) => this.groupsApi.getGroup(g.id).pipe(catchError(() => of(g)))),
          );
        }),
        catchError(() => of([] as Group[])),
      )
      .subscribe({
        next: (fullGroups) => {
          const currentUserId = this.iamStore.currentUserId();
          const currentUsername = this.iamStore.currentUsername();

          const leaderList = fullGroups.filter((grp) =>
            grp.usersInGroup?.some((u: any) => {
              const usr = u.user ?? u;
              const matchUser =
                (currentUserId && usr.id === currentUserId) ||
                (currentUsername && usr.username === currentUsername);
              return matchUser && u.roleInGroup === 'GROUP_LEADER';
            }),
          );

          const memberList = fullGroups.filter((grp) =>
            grp.usersInGroup?.some((u: any) => {
              const usr = u.user ?? u;
              const matchUser =
                (currentUserId && usr.id === currentUserId) ||
                (currentUsername && usr.username === currentUsername);
              return matchUser && u.roleInGroup === 'GROUP_MEMBER';
            }),
          );

          this.leaderGroupsSignal.set(leaderList.length > 0 ? leaderList : fullGroups);
          this.memberGroupsSignal.set(memberList.length > 0 ? memberList : fullGroups);
          this.groupsSignal.set(fullGroups);
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.errorSignal.set(this.formatError(err, 'Failed to load groups'));
          this.loadingSignal.set(false);
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
          this.errorSignal.set(this.formatError(err, 'Failed to create group'));
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
          this.errorSignal.set(this.formatError(err, 'Failed to update group'));
          this.loadingSignal.set(false);
        },
      });
  }

  private formatError(error: any, fallback: string): string {
    if (error instanceof Error) {
      return error.message.includes('Resource not found')
        ? `${fallback}: Not found`
        : error.message;
    }
    return fallback;
  }
}
