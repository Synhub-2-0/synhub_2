import { ToastStore } from '../../shared/application/toast.store';
import {computed, Injectable, signal, inject, effect } from '@angular/core';
import {Invitation} from '../domain/model/invitation.entity';
import {CreateInvitationCommand} from '../domain/model/create-invitation.command';
import {InvitationsApi} from '../infrastructure/invitations.api';
import {catchError, retry, switchMap, throwError} from 'rxjs';
import {IamApi} from '../../iam/infrastructure/iam.api';
import {IamStore} from '../../iam/application/iam.store';

@Injectable({providedIn: 'root'})
export class InvitationsStore {
  private readonly toastStore = inject(ToastStore);

  readonly invitationCount = computed(() => this.invitations().length);

  private readonly invitationsSignal = signal<Invitation[]>([]);
  private readonly groupInvitationsSignal = signal<Invitation[]>([]);
  private readonly loadingSignal = signal<boolean>(false);
  private readonly invitingSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly invitations = this.invitationsSignal.asReadonly();
  readonly groupInvitations = this.groupInvitationsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly inviting = this.invitingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  constructor(
    private invitationsApi: InvitationsApi,
    private iamApi: IamApi,
    private iamStore: IamStore,
  ) {
    // Root stores outlive the session: drop the previous user's data as soon as the session ends.
    effect(() => {
      if (!this.iamStore.isSignedIn()) this.reset();
    });
    if (localStorage.getItem('token')) {
      this.loadInvitations();
    }
  }

  reset(): void {
    this.invitationsSignal.set([]);
    this.groupInvitationsSignal.set([]);
    this.errorSignal.set(null);
  }

  getInvitationById(id: number) {
    return computed(() => this.invitations().find((i) => i.id === id));
  }

  loadInvitations(): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.invitationsApi
      .getInvitationsByAuthenticatedUser()
      .subscribe({
        next: (invitations) => {
          this.invitationsSignal.set(invitations);
          this.loadingSignal.set(false);
          this.errorSignal.set(null);
        },
        error: (err) => {
          this.setError(this.formatError(err, 'Failed to load invitations'));
          this.loadingSignal.set(false);
        }
      });
  }

  loadGroupInvitations(groupId: number): void {
    this.errorSignal.set(null);
    this.groupInvitationsSignal.set([]);
    this.invitationsApi
      .getInvitationsByGroupId(groupId)
      .subscribe({
        next: (invitations) => this.groupInvitationsSignal.set(invitations),
        error: (err) => this.setError(this.formatError(err, 'Failed to load group invitations')),
      });
  }

  /**
   * The backend only accepts a numeric user id, so the username is resolved first and then the invitation is created.
   */
  inviteByUsername(username: string, groupId: number, onSuccess?: () => void): void {
    const trimmedUsername = username.trim();
    if (!trimmedUsername) return;
    if (trimmedUsername === this.iamStore.currentUsername()) {
      this.setError('No puedes invitarte a ti mismo.');
      return;
    }

    this.invitingSignal.set(true);
    this.errorSignal.set(null);
    this.iamApi
      .getProfileByUsername(trimmedUsername)
      .pipe(
        catchError((err: Error) => throwError(() => new Error(
          err.message.includes('Resource not found')
            ? 'No existe un usuario con ese nombre.'
            : 'No se pudo buscar al usuario.'
        ))),
        switchMap((profile) =>
          this.invitationsApi.createInvitation(new CreateInvitationCommand({groupId, userId: profile.id}))
        ),
      )
      .subscribe({
        next: (createdInvitation) => {
          this.groupInvitationsSignal.update((invitations) => [...invitations, createdInvitation]);
          this.invitingSignal.set(false);
          onSuccess?.();
        },
        error: (err) => {
          this.setError(this.formatError(err, 'Failed to create invitation'));
          this.invitingSignal.set(false);
        }
      });
  }

  /*
   * TODO: Reactivar cuando el backend permita solicitar la unión a un grupo.
   *
   * requestInvitation(groupId: number, userId: number): void {
   *   this.loadingSignal.set(true);
   *   this.errorSignal.set(null);
   *   this.invitationsApi.inviteUserToGroup(groupId, userId).pipe(retry(2)).subscribe({
   *     next: invitation => {
   *       this.invitationsSignal.update(invitations => [...invitations, invitation]);
   *       this.loadingSignal.set(false);
   *     },
   *     error: err => {
   *       this.setError(this.formatError(err, 'Failed to request group invitation'));
   *       this.loadingSignal.set(false);
   *     },
   *   });
   * }
   */

  acceptInvitation(id: number, onSuccess?: () => void): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.invitationsApi
      .acceptInvitation(id)
      .pipe(retry(2))
      .subscribe({
        next: () => {
          this.removeInvitation(id);
          this.loadingSignal.set(false);
          onSuccess?.();
        },
        error: (err) => {
          this.setError(this.formatError(err, 'Failed to accept invitation'));
          this.loadingSignal.set(false);
        }
      });
  }

  // Used by the invited user to reject an invitation and by the leader to cancel one.
  declineInvitation(id: number): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.invitationsApi
      .declineInvitation(id)
      .pipe(retry(2))
      .subscribe({
        next: () => {
          this.removeInvitation(id);
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.setError(this.formatError(err, 'Failed to decline invitation'));
          this.loadingSignal.set(false);
        }
      });
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  private removeInvitation(id: number): void {
    this.invitationsSignal.update((invitations) => invitations.filter((i) => i.id !== id));
    this.groupInvitationsSignal.update((invitations) => invitations.filter((i) => i.id !== id));
  }

  private formatError(error: any, fallback: string): string {
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
