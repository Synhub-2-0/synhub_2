import {BaseResource, BaseResponse} from '../../../shared/infrastructure/base-response';

export interface SignInResource extends BaseResource {
  id: number;
  username: string;
  role: string[];
  /** Vencimiento de la sesión (ISO-8601). El token va en la cookie HttpOnly, no en el cuerpo. */
  expiresAt: string;
}

export interface SignInResponse extends BaseResponse, SignInResource {}
