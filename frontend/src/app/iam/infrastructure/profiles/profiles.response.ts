import {BaseResource, BaseResponse} from '../../../shared/infrastructure/base-response';

export interface ProfileResource extends BaseResource {
  id: number;
  name: string;
  surname: string;
  email: string;
  imgUrl: string;
  username?: string;
}

export interface ProfileResponse extends BaseResponse {
  profile: ProfileResource[];
}
