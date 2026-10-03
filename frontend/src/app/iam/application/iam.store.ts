import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Profile } from '../domain/model/profile.entity';
import { SignInCommand } from '../domain/model/sign-in.command';
import { SignUpCommand } from '../domain/model/sign-up.command';
import { User } from '../domain/model/user.entity';
import { IamApi } from '../infrastructure/iam.api';

@Injectable({ providedIn: 'root' })
export class IamStore {
  private readonly isSignedInSignal = signal(!!localStorage.getItem('token'));
  private readonly currentUsernameSignal = signal<string | null>(null);
  private readonly currentUserIdSignal = signal<number | null>(
    localStorage.getItem('userId') ? Number(localStorage.getItem('userId')) : null,
  );
  private readonly currentProfileSignal = signal<Profile | null>(null);
  private readonly currentTokenSignal = signal<string | null>(localStorage.getItem('token'));
  private readonly usersSignal = signal<User[]>([]);

  readonly isSignedIn = this.isSignedInSignal.asReadonly();
  readonly users = this.usersSignal.asReadonly();
  readonly currentUsername = this.currentUsernameSignal.asReadonly();
  readonly currentUserId = this.currentUserIdSignal.asReadonly();
  readonly currentProfile = this.currentProfileSignal.asReadonly();
  readonly currentToken = this.currentTokenSignal.asReadonly();
  readonly loadingUsers = signal(false);
  readonly isLoadingUsers = this.loadingUsers.asReadonly();

  constructor(private readonly iamApi: IamApi) {}

  signIn(command: SignInCommand, router: Router): void {
    this.clearSession();
    this.iamApi.signIn(command).subscribe({
      next: resource => {
        localStorage.setItem('token', resource.token);
        localStorage.setItem('userId', String(resource.id));
        this.currentTokenSignal.set(resource.token);
        this.isSignedInSignal.set(true);
        this.currentUsernameSignal.set(resource.username);
        this.currentUserIdSignal.set(resource.id);

        this.iamApi.getProfileByUserId(resource.id).subscribe({
          next: profile => this.currentProfileSignal.set(profile),
          error: err => {
            console.error('Failed to load profile:', err);
            this.currentProfileSignal.set(null);
          },
        });
        router.navigate(['/home']).then();
      },
      error: err => {
        console.error('Sign-in failed:', err);
        this.clearSession();
        router.navigate(['/auth/sign-in']).then();
      },
    });
  }

  signUp(command: SignUpCommand, router: Router): void {
    this.iamApi.signUp(command).subscribe({
      next: () => router.navigate(['/auth/sign-in']).then(),
      error: err => {
        console.error('Sign-up failed:', err);
        this.clearSession();
        router.navigate(['/auth/sign-up']).then();
      },
    });
  }

  signOut(router: Router): void {
    this.clearSession();
    router.navigate(['/auth/sign-in']).then();
  }

  loadUsers(): void {
    this.loadingUsers.set(true);
  }

  restoreSession(): Promise<void> {
    if (!localStorage.getItem('token')) return Promise.resolve();

    return new Promise(resolve => {
      this.iamApi.autoSignIn().subscribe({
        next: user => {
          this.isSignedInSignal.set(true);
          this.currentUsernameSignal.set(user.username);
          this.currentUserIdSignal.set(user.id);
          localStorage.setItem('userId', String(user.id));
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
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    this.currentTokenSignal.set(null);
    this.isSignedInSignal.set(false);
    this.currentUsernameSignal.set(null);
    this.currentUserIdSignal.set(null);
    this.currentProfileSignal.set(null);
  }
}
