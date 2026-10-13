package com.hub.synhub_backend.groups.domain.services;

import com.hub.synhub_backend.groups.domain.model.aggregates.Invitation;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationsByUserIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationsByGroupIdQuery;

import java.util.List;
import java.util.Optional;

public interface InvitationQueryService {
    List<Invitation> handle(GetInvitationsByUserIdQuery query);
    List<Invitation> handle(GetInvitationsByGroupIdQuery query);
}
