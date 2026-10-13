import {BaseAssembler} from '../../../shared/infrastructure/base-assembler';
import {Profile} from '../../domain/model/profile.entity';
import {ProfileResource, ProfileResponse} from './profiles.response';

export class ProfilesAssembler implements BaseAssembler<Profile, ProfileResource, ProfileResponse> {
  toEntityFromResource(resource: ProfileResource): Profile {
    return new Profile({
      id: resource.id,
      name: resource.name,
      surname: resource.surname,
      email: resource.email,
      imgUrl: resource.imgUrl,
      // El backend no envía userId: el perfil es el propio usuario (AUDIT BUG-15).
      userId: resource.id,
      username: resource.username,
    })
  }

  toResourceFromEntity(entity: Profile): ProfileResource {
    return {
      id: entity.id,
      name: entity.name,
      surname: entity.surname,
      email: entity.email,
      imgUrl: entity.imgUrl,
      username: entity.username,
    } as ProfileResource;
  }

  toEntitiesFromResponse(response: ProfileResponse): Profile[] {
    return response.profile.map(profileResource => this.toEntityFromResource(profileResource));
  }
}
