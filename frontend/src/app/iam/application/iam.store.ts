import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Profile } from '../domain/model/profile.entity';
import { SignInCommand } from '../domain/model/sign-in.command';
import { SignUpCommand } from '../domain/model/sign-up.command';
import { User } from '../domain/model/user.entity';
import { IamApi } from '../infrastructure/iam.api';
import { ToastStore } from '../../shared/application/toast.store';
import { clearStoredSession, hasActiveSession, saveSession, storedUserId } from '../infrastructure/session';

@Injectable({ providedIn: 'root' })
export class IamStore {
  // El JWT vive en una cookie HttpOnly (HUM-04); aquí solo se conoce si hay una sesión vigente.
  private readonly isSignedInSignal = signal(hasActiveSession());
  private readonly currentUsernameSignal = signal<string | null>(null);
  private readonly currentUserIdSignal = signal<number | null>(hasActiveSession() ? storedUserId() : null);
  private readonly currentProfileSignal = signal<Profile | null>(null);
  private readonly usersSignal = signal<User[]>([]);

  readonly isSignedIn = this.isSignedInSignal.asReadonly();
  readonly users = this.usersSignal.asReadonly();
  readonly currentUsername = this.currentUsernameSignal.asReadonly();
  readonly currentUserId = this.currentUserIdSignal.asReadonly();
  readonly currentProfile = this.currentProfileSignal.asReadonly();
  readonly loadingUsers = signal(false);
  readonly isLoadingUsers = this.loadingUsers.asReadonly();

  private readonly toastStore = inject(ToastStore);

  constructor(private readonly iamApi: IamApi) {}

  signIn(command: SignInCommand, router: Router): void {
    this.clearSession();
    this.iamApi.signIn(command).subscribe({
      next: resource => {
        saveSession(resource.id, resource.expiresAt);
        this.isSignedInSignal.set(true);
        this.currentUsernameSignal.set(resource.username);
        this.currentUserIdSignal.set(resource.id);

        this.iamApi.getProfileByUserId(resource.id).subscribe({
          next: profile => this.currentProfileSignal.set(profile),
          error: () => this.currentProfileSignal.set(null),
        });
        router.navigate(['/home']).then();
      },
      error: err => {
        this.toastStore.error(err.message);
        this.clearSession();
        router.navigate(['/auth/sign-in']).then();
      },
    });
  }

  signUp(command: SignUpCommand, router: Router): void {
    this.iamApi.signUp(command).subscribe({
      // The new account is signed in right away with the credentials just registered.
      next: () => this.signIn(new SignInCommand({ username: command.username, password: command.password }), router),
      error: err => {
        this.toastStore.error(err.message);
        this.clearSession();
        router.navigate(['/auth/sign-up']).then();
      },
    });
  }

  /** Borra la cookie en el backend y la sesión local; navega al login aunque la petición falle. */
  signOut(router: Router): void {
    const leave = () => {
      this.clearSession();
      router.navigate(['/auth/sign-in']).then();
    };
    this.iamApi.signOut().subscribe({ next: leave, error: leave });
  }

  /** Cierra la sesión local sin llamar a la API (sesión vencida o rechazada con 401). */
  endSession(): void {
    this.clearSession();
  }

  loadUsers(): void {
    this.loadingUsers.set(true);
  }

  restoreSession(): Promise<void> {
    if (!hasActiveSession()) {
      this.clearSession();
      return Promise.resolve();
    }

    return new Promise(resolve => {
      this.iamApi.autoSignIn().subscribe({
        next: user => {
          this.isSignedInSignal.set(true);
          this.currentUsernameSignal.set(user.username);
          this.currentUserIdSignal.set(user.id);
          this.iamApi.getProfileByUserId(user.id).subscribe({
            next: profile => this.currentProfileSignal.set(profile),
            error: () => this.currentProfileSignal.set(null),
            complete: () => resolve(),
          });
        },
        error: () => {
          this.clearSession();
          resolve();
        },
      });
    });
  }

  private clearSession(): void {
    clearStoredSession();
    this.isSignedInSignal.set(false);
    this.currentUsernameSignal.set(null);
    this.currentUserIdSignal.set(null);
    this.currentProfileSignal.set(null);
  }
}
