import {computed, Injectable, Signal, signal} from '@angular/core';
import {Group} from '../domain/model/group.entity';
import {GroupsApi} from '../infrastructure/groups.api';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {retry} from 'rxjs';

@Injectable({providedIn: 'root'})
export class GroupsStore {

  readonly groupCount = computed(() => this.groups().length);

  private readonly groupsSignal = signal<Group[]>([]);
  private readonly loadingSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly groups = this.groupsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  constructor(private groupsApi: GroupsApi) {
    this.loadGroups();
  }

  getGroupById(id: number): Signal<Group | undefined> {
    return computed(() => (id ? this.groups().find((g) => g.id === id) : undefined));
  }

  addGroup(group: Group): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.groupsApi
      .createGroup(group)
      .pipe(retry(2))
      .subscribe({
        next: (createdGroup) => {
          this.groupsSignal.update((groups) => [...groups, createdGroup]);
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.errorSignal.set(this.formatError(err, 'Failed to create group'));
          this.loadingSignal.set(false);
        }
      })
  }

  updateGroup(updatedGroup: Group): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.groupsApi
      .updateGroup(updatedGroup)
      .pipe(retry(2))
      .subscribe({
        next: (group) => {
          this.groupsSignal.update((groups) =>
            groups.map((g) => (g.id === group.id ? group : g))
          );
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.errorSignal.set(this.formatError(err, 'Failed to update group'));
          this.loadingSignal.set(false);
        }
      })
  }

  private loadGroups(): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.groupsApi
      .getGroupsByUser()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (groups) => {
          this.groupsSignal.set(groups);
          this.loadingSignal.set(false);
          this.errorSignal.set(null);
        },
        error: (err) => {
          this.errorSignal.set(this.formatError(err, 'Failed to load groups'));
          this.loadingSignal.set(false);
        }
      })
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
