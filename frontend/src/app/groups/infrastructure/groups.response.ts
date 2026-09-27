import {BaseResource, BaseResponse} from '../../shared/infrastructure/base-response';
import {ProfileResource} from '../../iam/infrastructure/profiles/profiles.response';

export interface GroupResource extends BaseResource {
  id: number;
  name: string;
  imgUrl: string;
  description: string;
  code: string;
  usersInGroup?: GroupUsersResource[];
  memberCount?: number;
  leader?: ProfileResource;
}

export interface GroupUsersResource {
  user: ProfileResource;
  roleInGroup: string;
}

export interface GroupsResponse extends BaseResponse {
  groups: GroupResource[];
}
