import {BaseResource, BaseResponse} from '../../../shared/infrastructure/base-response';

export interface ProfileResource extends BaseResource {
  id: number;
  name: string;
  surname: string;
  email: string;
  imgUrl: string;
  userId: number;
}

export interface ProfileResponse extends BaseResponse {
  profile: ProfileResource[];
}
