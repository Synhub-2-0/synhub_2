import {BaseResource, BaseResponse} from '../../shared/infrastructure/base-response';
import {UserResource} from '../../iam/infrastructure/users.response';

export interface GroupResource extends BaseResource {
  id: number;
  name: string;
  imgUrl: string;
  description: string;
  code: string;
  usersInGroup: GroupUsersResource[];
}

export interface GroupUsersResource {
  user: UserResource;
  //user: ProfileResource;
  roleInGroup: string;
}

export interface GroupsResponse extends BaseResponse {
  groups: GroupResource[];
}
