import {environment} from '../../../../environments/environment';
import {BaseApiEndpoint} from '../../../shared/infrastructure/base-api-endpoint';
import {User} from '../../domain/model/user.entity';
import {UserResource, UsersResponse} from './users.response';
import {UsersAssembler} from './users.assembler';
import {HttpClient} from '@angular/common/http';
import {catchError, map, Observable} from 'rxjs';

const usersApiEndpointUrl = `${environment.platformProviderApiBaseUrl}${environment.platformProviderUserEndpointPath}`;

export class UsersApiEndpoint extends BaseApiEndpoint<User, UserResource, UsersResponse, UsersAssembler> {
  constructor(http: HttpClient) {
    super(http, usersApiEndpointUrl, new UsersAssembler());
  }

  getByAuthentication(): Observable<User> {
    return this.http.get<UserResource>(`${usersApiEndpointUrl}/me`).pipe(
      map(resource => this.assembler.toEntityFromResource(resource)),
      catchError(this.handleError('Failed to fetch user by the provided authentication'))
    );
  }

  getByUsername(username: string): Observable<User> {
    return this.http.get<UserResource>(`${usersApiEndpointUrl}/username?username=${encodeURIComponent(username)}`).pipe(
      map(resource => this.assembler.toEntityFromResource(resource)),
      catchError(this.handleError('Failed to fetch user by username'))
    );
  }
}
