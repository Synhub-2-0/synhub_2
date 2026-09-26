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
      userId: resource.userId,
    })
  }

  toResourceFromEntity(entity: Profile): ProfileResource {
    return {
      id: entity.id,
      name: entity.name,
      surname: entity.surname,
      email: entity.email,
      imgUrl: entity.imgUrl,
      userId: entity.userId,
    } as ProfileResource;
  }

  toEntitiesFromResponse(response: ProfileResponse): Profile[] {
    return response.profile.map(profileResource => this.toEntityFromResource(profileResource));
  }
}
