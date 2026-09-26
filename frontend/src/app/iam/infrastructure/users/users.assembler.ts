import {BaseAssembler} from '../../../shared/infrastructure/base-assembler';
import {User} from '../../domain/model/user.entity';
import {UserResource, UsersResponse} from './users.response';

export class UsersAssembler implements BaseAssembler<User, UserResource, UsersResponse> {
  toEntityFromResource(resource: UserResource): User {
    return new User({
      id: resource.id,
      username: resource.username,
      accountRoles: resource.accountRoles,
    });
  }

  toResourceFromEntity(entity: User): UserResource {
    return {
      id: entity.id,
      username: entity.username,
      accountRoles: entity.accountRoles,
    } as UserResource;
  }

  toEntitiesFromResponse(response: UsersResponse): User[] {
    return response.users.map(userResource => this.toEntityFromResource(userResource));
  }
}
