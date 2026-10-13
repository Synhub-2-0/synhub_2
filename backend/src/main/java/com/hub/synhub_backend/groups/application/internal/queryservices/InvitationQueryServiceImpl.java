package com.hub.synhub_backend.groups.application.internal.queryservices;

import com.hub.synhub_backend.groups.domain.model.aggregates.Invitation;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationsByUserIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetInvitationsByGroupIdQuery;
import com.hub.synhub_backend.groups.domain.services.InvitationQueryService;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.InvitationRepository;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InvitationQueryServiceImpl implements InvitationQueryService {

    private final InvitationRepository invitationRepository;
    private final GroupRepository groupRepository;

    public InvitationQueryServiceImpl(InvitationRepository invitationRepository, GroupRepository groupRepository) {
        this.invitationRepository = invitationRepository;
        this.groupRepository = groupRepository;
    }

    @Override
    public List<Invitation> handle(GetInvitationsByUserIdQuery query) {
        return this.invitationRepository.findByUser_Id(query.userId());
    }

    @Override
    public List<Invitation> handle(GetInvitationsByGroupIdQuery query) {
        var group = this.groupRepository.findById(query.groupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group with id " + query.groupId() + " does not exist"));
        var leader = group.getLeader();
        if (leader == null || !leader.getId().equals(query.requesterId())) {
            throw new AccessDeniedException("Only the leader of the group can list its invitations");
        }
        return this.invitationRepository.findByGroup_Id(query.groupId());
    }
}
