import {computed, Injectable, signal} from '@angular/core';
import {User} from '../domain/model/user.entity';
import {SignInCommand} from '../domain/model/sign-in.command';
import {Router} from '@angular/router';
import {IamApi} from '../infrastructure/iam.api';
import {SignUpCommand} from '../domain/model/sign-up.command';
import {Profile} from '../domain/model/profile.entity';

@Injectable({providedIn: 'root'})
export class IamStore {
  private readonly isSignedInSignal = signal<boolean>(false);
  private readonly currentUsernameSignal = signal<string | null>(null);
  private readonly currentUserIdSignal = signal<number | null>(null);
  private readonly currentProfileSignal = signal<Profile | null>(null);
  private readonly usersSignal = signal<Array<User>>([]);

  readonly isSignedIn = this.isSignedInSignal.asReadonly();
  readonly users = this.usersSignal.asReadonly();

  readonly currentUsername = this.currentUsernameSignal.asReadonly();
  readonly currentUserId = this.currentUserIdSignal.asReadonly();
  readonly currentProfile = this.currentProfileSignal.asReadonly();
  readonly currentToken = computed(() => localStorage.getItem('token'));

  readonly loadingUsers = signal<boolean>(false);
  readonly isLoadingUsers = this.loadingUsers.asReadonly();

  constructor(private iamApi: IamApi) {
    this.isSignedInSignal.set(false);
    this.currentUsernameSignal.set(null);
    this.currentUserIdSignal.set(null);
  }

  signIn(signInCommand: SignInCommand, router: Router) {
    console.log(signInCommand);
    this.iamApi.signIn(signInCommand).subscribe({
      next: (signInResource) => {
        localStorage.setItem('token', signInResource.token);
        this.isSignedInSignal.set(true);
        this.currentUsernameSignal.set(signInResource.username);
        this.currentUserIdSignal.set(signInResource.id);

        this.iamApi.getProfileByUserId(signInResource.id).subscribe({
          next: (profile) => {
            this.currentProfileSignal.set(profile);
          },
          error: (err) => {
            console.error('Failed to load profile:', err);
            this.currentProfileSignal.set(null);
          }
        });

        router.navigate(['/home']).then();
      },
      error: (err) => {
        console.error('Sign-in failed:', err);
        this.isSignedInSignal.set(false);
        this.currentUsernameSignal.set(null);
        this.currentUserIdSignal.set(null);
        router.navigate(['/auth/sign-in']).then();
      }
    });
  }

  signUp(signUpCommand: SignUpCommand, router: Router) {
    this.iamApi.signUp(signUpCommand).subscribe({
      next: (signUpResource) => {
        console.log('Sign-up successful:', signUpResource);
        router.navigate(['/auth/sign-in']).then();
      },
      error: (err) => {
        console.error('Sign-up failed:', err);
        this.isSignedInSignal.set(false);
        this.currentUsernameSignal.set(null);
        this.currentUserIdSignal.set(null);
        this.currentProfileSignal.set(null);
        router.navigate(['/auth/sign-up']).then();
      }
    });
  }

  signOut(router: Router) {
    localStorage.removeItem('token');
    this.isSignedInSignal.set(false);
    this.currentUsernameSignal.set(null);
    this.currentUserIdSignal.set(null);
    this.currentProfileSignal.set(null);
    router.navigate(['/auth/sign-in']).then();
  }

  loadUsers() {
    this.loadingUsers.set(true);
    // TODO: Implement user loading logic when profile is implemented
  }

  restoreSession(): Promise<void> {
    if (!this.currentToken()) {
      return Promise.resolve();
    }

    return new Promise<void>((resolve) => {
      this.iamApi.autoSignIn().subscribe({
        next: (user) => {
          this.isSignedInSignal.set(true);
          this.currentUsernameSignal.set(user.username);
          this.currentUserIdSignal.set(user.id);

          // load profile (optionally wait for it)
          this.iamApi.getProfileByUserId(user.id).subscribe({
            next: (profile) => this.currentProfileSignal.set(profile),
            error: () => this.currentProfileSignal.set(null),
            complete: () => resolve()
          });
        },
        error: () => {
          localStorage.removeItem('token');
          this.isSignedInSignal.set(false);
          this.currentUsernameSignal.set(null);
          this.currentUserIdSignal.set(null);
          this.currentProfileSignal.set(null);
          resolve();
        }
      });
    });
  }
}
