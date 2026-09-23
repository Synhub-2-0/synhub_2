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

@Injectable({providedIn: 'root'})
export class IamApi extends BaseApi {
  private readonly signUpEndpoint: SignUpApiEndpoint;
  private readonly signInEndpoint: SignInApiEndpoint;

  constructor(http: HttpClient) {
    super();
    this.signUpEndpoint = new SignUpApiEndpoint(http, new SignUpAssembler());
    this.signInEndpoint = new SignInApiEndpoint(http, new SignInAssembler());
  }

  signUp(signUpCommand: SignUpCommand): Observable<SignUpResource>  {
    return this.signUpEndpoint.signUp(signUpCommand);
  }

  signIn(signInCommand: SignInCommand): Observable<SignInResource> {
    return this.signInEndpoint.signIn(signInCommand);
  }
}
