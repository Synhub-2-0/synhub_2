import {computed, Injectable, signal} from '@angular/core';
import {Invitation} from '../domain/model/invitation.entity';
import {InvitationsApi} from '../infrastructure/invitations.api';
import {retry} from 'rxjs';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';

@Injectable({providedIn: 'root'})
export class InvitationsStore {

  readonly invitationCount = computed(() => this.invitations().length);

  private readonly invitationsSignal = signal<Invitation[]>([]);
  private readonly loadingSignal = signal<boolean>(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly invitations = this.invitationsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  constructor(private invitationsApi: InvitationsApi) {
    this.loadInvitations();
  }

  getInvitationById(id: number) {
    return computed(() => this.invitations().find((i) => i.id === id));
  }

  addInvitation(invitation: Invitation): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.invitationsApi
      .createInvitation(invitation)
      .pipe(retry(2))
      .subscribe({
        next: (createdInvitation) => {
          this.invitationsSignal.update((invitations) => [...invitations, createdInvitation]);
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.errorSignal.set(this.formatError(err, 'Failed to create invitation'));
          this.loadingSignal.set(false);
        }
      });
  }

  acceptInvitation(id: number): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.invitationsApi
      .acceptInvitation(id)
      .pipe(retry(2))
      .subscribe({
        next: () => {
          this.invitationsSignal.update((invitations) =>
            invitations.filter((i) => i.id !== id)
          );
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.errorSignal.set(this.formatError(err, 'Failed to accept invitation'));
          this.loadingSignal.set(false);
        }
      });
  }

  declineInvitation(id: number): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.invitationsApi
      .declineInvitation(id)
      .pipe(retry(2))
      .subscribe({
        next: () => {
          this.invitationsSignal.update((invitations) =>
            invitations.filter((i) => i.id !== id)
          );
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.errorSignal.set(this.formatError(err, 'Failed to decline invitation'));
          this.loadingSignal.set(false);
        }
      });
  }

  private loadInvitations(): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.invitationsApi
      .getInvitationsByAuthenticatedUser()
      .pipe(takeUntilDestroyed())
      .subscribe({
        next: (invitations) => {
          this.invitationsSignal.set(invitations);
          this.loadingSignal.set(false);
          this.errorSignal.set(null);
        },
        error: (err) => {
          this.errorSignal.set(this.formatError(err, 'Failed to load invitations'));
          this.loadingSignal.set(false);
        }
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
