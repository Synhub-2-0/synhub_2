import {BaseAssembler} from '../../shared/infrastructure/base-assembler';
import {Group} from '../domain/model/group.entity';
import {GroupResource, GroupUsersResource, GroupsResponse} from './groups.response';
import {GroupUser} from '../domain/model/group-user.entity';

export class GroupAssembler implements BaseAssembler<Group, GroupResource, GroupsResponse> {
  // private profileAssembler = new ProfileAssembler();

  toEntitiesFromResponse(response: GroupsResponse): Group[] {
    return response.groups.map(resource => this.toEntityFromResource(resource as GroupResource))
  }

  toEntityFromResource(resource: GroupResource): Group {
    return new Group({
      id: resource.id,
      name: resource.name,
      imgUrl: resource.imgUrl,
      description: resource.description,
      code: resource.code,
      usersInGroup: resource.usersInGroup.map(gu => this.toGroupUserFromResource(gu))
    })
  }

  toGroupUserFromResource(resource: GroupUsersResource): GroupUser {
    return new GroupUser({
      user: resource.user,
      roleInGroup: resource.roleInGroup
    })
  }
}
