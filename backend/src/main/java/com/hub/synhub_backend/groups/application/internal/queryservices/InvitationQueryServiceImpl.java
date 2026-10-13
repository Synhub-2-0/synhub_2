package com.hub.synhub_backend.groups.application.internal.queryservices;

import com.hub.synhub_backend.groups.domain.model.aggregates.Invitation;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationsByUserIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationsByGroupIdQuery;
import com.hub.synhub_backend.groups.domain.services.InvitationQueryService;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.InvitationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InvitationQueryServiceImpl implements InvitationQueryService {

    private final InvitationRepository invitationRepository;

    public InvitationQueryServiceImpl(InvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    @Override
    public List<Invitation> handle(GetInvitationsByUserIdQuery query) {
        return this.invitationRepository.findByUser_Id(query.userId());
    }

    @Override
    public List<Invitation> handle(GetInvitationsByGroupIdQuery query) {
        return this.invitationRepository.findByGroup_Id(query.groupId());
    }
}
