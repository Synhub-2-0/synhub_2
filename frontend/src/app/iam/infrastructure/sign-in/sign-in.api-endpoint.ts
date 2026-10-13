import {environment} from '../../../../environments/environment';
import {HttpClient} from '@angular/common/http';
import {SignInAssembler} from './sign-in.assembler';
import {SignInCommand} from '../../domain/model/sign-in.command';
import {catchError, map, Observable} from 'rxjs';
import {SignInResponse, SignInResource} from './sign-in.response';
import {ErrorHandlingEnabledBaseType} from '../../../shared/infrastructure/error-handling-enabled-base-type';

const signInApiEndpointUrl = `${environment.platformProviderApiBaseUrl}${environment.platformProviderSignInEndpointPath}`;
const signOutApiEndpointUrl = `${environment.platformProviderApiBaseUrl}${environment.platformProviderSignOutEndpointPath}`;

export class SignInApiEndpoint extends ErrorHandlingEnabledBaseType {
  constructor(private http: HttpClient, private assembler: SignInAssembler) {
    super();
  }

  signIn(signInCommand: SignInCommand): Observable<SignInResource> {
    const signInRequest = this.assembler.toRequestFromCommand(signInCommand);
    return this.http.post<SignInResponse>(signInApiEndpointUrl, signInRequest).pipe(
      map(response => this.assembler.toResourceFromResponse(response)),
      catchError(this.handleError('Failed to sign-in'))
    );
  }

  /** Pide al backend que borre la cookie de sesión. */
  signOut(): Observable<void> {
    return this.http.post<void>(signOutApiEndpointUrl, null).pipe(
      catchError(this.handleError('Failed to sign-out'))
    );
  }
}
