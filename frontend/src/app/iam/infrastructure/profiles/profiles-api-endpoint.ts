import {environment} from '../../../../environments/environment';
import {BaseApiEndpoint} from '../../../shared/infrastructure/base-api-endpoint';
import {Profile} from '../../domain/model/profile.entity';
import {ProfileResource, ProfileResponse} from './profiles-response';
import {ProfilesAssembler} from './profiles-assembler';
import {HttpClient} from '@angular/common/http';
import {catchError, map, Observable} from 'rxjs';

const profilesApiEndpointUrl = `${environment.platformProviderApiBaseUrl}${environment.platformProviderProfileEndpointPath}`;
// TODO: Temp
const usersApiEndpointUrl = `${environment.platformProviderApiBaseUrl}/api/v1/users`;

export class ProfilesApiEndpoint extends BaseApiEndpoint<Profile, ProfileResource, ProfileResponse, ProfilesAssembler> {
  constructor(http: HttpClient) {
    super( http, profilesApiEndpointUrl, new ProfilesAssembler());
  }

  getByUserId(userId: number): Observable<Profile> {
    return this.http.get<ProfileResource>(`${usersApiEndpointUrl}/${userId}`).pipe(
      map(resource => this.assembler.toEntityFromResource(resource)),
      catchError(this.handleError('Failed to fetch profile by the provided user'))
    );
  }
}
