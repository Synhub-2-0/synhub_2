import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {BaseApi} from '../../shared/infrastructure/base-api';
import {SignUpApiEndpoint} from './sign-up/sign-up.api-endpoint';
import {SignInApiEndpoint} from './sign-in/sign-in.api-endpoint';
import {SignUpAssembler} from './sign-up/sign-up.assembler';
import {SignInAssembler} from './sign-in/sign-in.assembler';
import {SignUpCommand} from '../domain/model/sign-up.command';
import {SignInCommand} from '../domain/model/sign-in.command';
import {SignUpResource} from './sign-up/sign-up.response';
import {SignInResource} from './sign-in/sign-in.response';
import {ProfilesApiEndpoint} from './profiles/profiles.api-endpoint';
import {Profile} from '../domain/model/profile.entity';
import {UserResource} from './users/users.response';
import {UsersApiEndpoint} from './users/users.api-endpoint';
import {UsersAssembler} from './users/users.assembler';

@Injectable({providedIn: 'root'})
export class IamApi extends BaseApi {
  private readonly signUpEndpoint: SignUpApiEndpoint;
  private readonly signInEndpoint: SignInApiEndpoint;
  private readonly usersEndpoint: UsersApiEndpoint;
  private readonly profilesEndpoint: ProfilesApiEndpoint;

  constructor(http: HttpClient) {
    super();
    this.signUpEndpoint = new SignUpApiEndpoint(http, new SignUpAssembler());
    this.signInEndpoint = new SignInApiEndpoint(http, new SignInAssembler());
    this.usersEndpoint = new UsersApiEndpoint(http);
    this.profilesEndpoint = new ProfilesApiEndpoint(http);
  }

  // Authentication
  signUp(signUpCommand: SignUpCommand): Observable<SignUpResource>  {
    return this.signUpEndpoint.signUp(signUpCommand);
  }

  signIn(signInCommand: SignInCommand): Observable<SignInResource> {
    return this.signInEndpoint.signIn(signInCommand);
  }

  // Users
  getUser(id: number): Observable<UserResource> {
    return this.usersEndpoint.getById(id);
  }

  autoSignIn(): Observable<UserResource> {
    return this.usersEndpoint.getByAuthentication();
  }

  getUserByUsername(username: string): Observable<UserResource> {
    return this.usersEndpoint.getByUsername(username);
  }

  // Profiles
  getProfile(id: number): Observable<Profile> {
    return this.profilesEndpoint.getById(id);
  }

  updateProfile(profile: Profile): Observable<Profile> {
    return this.profilesEndpoint.update(profile, profile.id);
  }

  getProfileByUserId(userId: number): Observable<Profile> {
    return this.profilesEndpoint.getByUserId(userId);
  }

  getProfileByUsername(username: string): Observable<Profile> {
    return this.profilesEndpoint.getByUsername(username);
  }
}
