import {BaseResource, BaseResponse} from '../../shared/infrastructure/base-response';
import {GroupResource} from '../../groups/infrastructure/groups.response';
import {ProfileResource} from '../../iam/infrastructure/profiles/profiles.response';

// Invitation endpoints depend mostly on IDs, hence why this is created when sending request bodies.
export interface CreateInvitationRequest {
  groupId: number;
  userId: number;
}

export interface InvitationResource extends BaseResource {
  id: number;
  group: GroupResource;
  user: ProfileResource;
}

export interface InvitationsResponse extends BaseResponse {
  invitations: InvitationResource[];
}
