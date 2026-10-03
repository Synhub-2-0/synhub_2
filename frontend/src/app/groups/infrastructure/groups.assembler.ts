import {BaseAssembler} from '../../shared/infrastructure/base-assembler';
import {Group} from '../domain/model/group.entity';
import {GroupResource, GroupUsersResource, GroupsResponse} from './groups.response';
import {GroupUser} from '../domain/model/group-user.entity';
import {ProfilesAssembler} from '../../iam/infrastructure/profiles/profiles.assembler';
import {CreateGroupCommand} from '../domain/model/create-group.command';
import {CreateGroupRequest} from './create-group.request';

export class GroupsAssembler implements BaseAssembler<Group, GroupResource, GroupsResponse> {
  private profileAssembler = new ProfilesAssembler();

  toEntitiesFromResponse(response: GroupsResponse): Group[] {
    return (response?.groups ?? []).map(resource => this.toEntityFromResource(resource as GroupResource));
  }

  toEntityFromResource(resource: GroupResource): Group {
    return new Group({
      id: resource.id,
      name: resource.name,
      imgUrl: resource.imgUrl,
      description: resource.description,
      code: resource.code,
      memberCount: resource.memberCount ?? 0
    });
  }

  toGroupUsersFromResources(resources: GroupUsersResource[]): GroupUser[] {
    return (resources ?? []).map(resource => this.toGroupUserFromResource(resource));
  }

  toGroupUserFromResource(resource: GroupUsersResource): GroupUser {
    return new GroupUser({
      user: this.profileAssembler.toEntityFromResource(resource.user),
      roleInGroup: resource.roleInGroup
    })
  }

  toResourceFromEntity(entity: Group): GroupResource {
    return {
      id: entity.id,
      name: entity.name,
      imgUrl: entity.imgUrl,
      description: entity.description,
      code: entity.code,
      memberCount: entity.memberCount
    } as GroupResource;
  }

  toRequestFromCreateCommand(command: CreateGroupCommand): CreateGroupRequest {
    return {
      name: command.name,
      description: command.description,
      imgUrl: command.imgUrl
    } as CreateGroupRequest;
  }
}
