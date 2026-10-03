import { computed, Injectable, Signal, signal } from '@angular/core';
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
  private readonly groupsSignal = signal<Group[]>([]);
  private readonly leaderGroupsSignal = signal<Group[]>([]);
  private readonly memberGroupsSignal = signal<Group[]>([]);
  private readonly membersByGroupSignal = signal<Record<number, GroupUser[]>>({});
  private readonly loadingSignal = signal<boolean>(false);
  private readonly membersLoadingSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly groups = this.groupsSignal.asReadonly();
  readonly leaderGroups = this.leaderGroupsSignal.asReadonly();
  readonly memberGroups = this.memberGroupsSignal.asReadonly();
  readonly membersByGroup = this.membersByGroupSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly membersLoading = this.membersLoadingSignal.asReadonly();
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
        this.errorSignal.set(this.formatError(err, 'Failed to load groups'));
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
        this.errorSignal.set(this.formatError(err, 'Failed to load group members'));
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
