package com.hub.synhub_backend.groups.domain.services;

import com.hub.synhub_backend.groups.domain.model.aggregates.Invitation;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationsByGroupIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationsByUserIdQuery;
import java.util.List;

public interface InvitationQueryService {
    List<Invitation> handle(GetInvitationsByUserIdQuery query);

    List<Invitation> handle(GetInvitationsByGroupIdQuery query);
}
