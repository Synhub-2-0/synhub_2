package com.hub.synhub_backend.groups.domain.services;

import com.hub.synhub_backend.groups.domain.model.aggregates.Invitation;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationByMemberIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationsByGroupIdQuery;

import java.util.List;
import java.util.Optional;

public interface InvitationQueryService {
    Optional<Invitation> handle(GetInvitationByMemberIdQuery query);
    List<Invitation> handle(GetInvitationsByGroupIdQuery query);
}
