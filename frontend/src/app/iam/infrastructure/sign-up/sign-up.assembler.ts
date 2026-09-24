import {SignUpRequest} from './sign-up.request';
import {SignUpCommand} from '../../domain/model/sign-up.command';
import {SignUpResource, SignUpResponse} from './sign-up.response';

export class SignUpAssembler {
  toResourceFromResponse(response: SignUpResponse): SignUpResource {
    return {
      id: response.id,
      username: response.username,
      name: response.name,
      surname: response.surname,
      imgUrl: response.imgUrl,
      email: response.email,
      accountRoles: response.accountRoles
    } as SignUpResource;
  }

  toRequestFromCommand(command: SignUpCommand): SignUpRequest {
    return {
      username: command.username,
      name: command.name,
      surname: command.surname,
      imgUrl: command.imgUrl,
      email: command.email,
      password: command.password,
      accountRoles: command.accountRoles,
    } as SignUpRequest;
  }
}
