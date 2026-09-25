import {BaseApiEndpoint} from '../../shared/infrastructure/base-api-endpoint';
import {Group} from '../domain/model/group.entity';
import {GroupResource, GroupsResponse} from './groups.response';
import {GroupAssembler} from './group.assembler';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../../environments/environment';

export class GroupsApiEndpoint extends BaseApiEndpoint<Group, GroupResource, GroupsResponse, GroupAssembler> {
  constructor(http: HttpClient) {
    super(http, `${environment.platformProviderApiBaseUrl}${environment.platformProviderGroupsEndpointPath}`, new GroupAssembler());
  }
}
