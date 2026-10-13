package com.hub.synhub_backend.groups.application.internal.acl;

import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.interfaces.acl.GroupsContextFacade;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GroupsContextFacadeImpl implements GroupsContextFacade {

    private final GroupQueryService groupQueryService;
    private final GroupProfileRepository groupProfileRepository;

    public GroupsContextFacadeImpl(GroupQueryService groupQueryService, GroupProfileRepository groupProfileRepository) {
        this.groupQueryService = groupQueryService;
        this.groupProfileRepository = groupProfileRepository;
    }

    @Override
    public Optional<Long> fetchLeaderIdByGroupId(Long groupId) {
        return groupQueryService
                .handle(new GetGroupByIdQuery(groupId))
                .flatMap(group -> Optional.ofNullable(group.getLeader()))
                .map(User::getId);
    }

    @Override
    public boolean isMember(Long groupId, Long userId) {
        return groupId != null && userId != null && groupProfileRepository.existsByGroup_IdAndUser_Id(groupId, userId);
    }

    @Override
    public boolean isLeader(Long groupId, Long userId) {
        if (groupId == null || userId == null) return false;
        return groupProfileRepository
                .findByGroup_IdAndUser_Id(groupId, userId)
                .map(profile -> profile.getRoleInGroup() == GroupRole.GROUP_LEADER)
                .orElse(false);
    }

    @Override
    public Optional<GroupProfile> fetchMembership(Long groupId, Long userId) {
        if (groupId == null || userId == null) return Optional.empty();
        return groupProfileRepository.findByGroup_IdAndUser_Id(groupId, userId);
    }
}
