import {BaseResource, BaseResponse} from '../../../shared/infrastructure/base-response';

export interface SignUpResource extends BaseResource {
  id: number;
  username: string;
  name: string;
  surname: string;
  imgUrl: string;
  email: string;
  accountRoles: string[];
}

export interface SignUpResponse extends BaseResponse, SignUpResource {}
