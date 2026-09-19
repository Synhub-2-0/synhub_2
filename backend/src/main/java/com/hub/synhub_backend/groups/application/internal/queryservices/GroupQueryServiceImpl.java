package com.hub.synhub_backend.groups.application.internal.queryservices;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByCodeQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupsByLeaderIdQuery;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupsByUserIdQuery;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupCode;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ListResourceBundle;
import java.util.Optional;

@Service
public class GroupQueryServiceImpl implements GroupQueryService {
    private final GroupRepository groupRepository;
    private final GroupProfileRepository groupProfileRepository;

    public GroupQueryServiceImpl(
            GroupRepository groupRepository,
            GroupProfileRepository groupProfileRepository) {
        this.groupRepository = groupRepository;
        this.groupProfileRepository = groupProfileRepository;
    }

    @Override
    public List<Group> handle(GetGroupsByLeaderIdQuery query) {
        return this.groupRepository.findByLeaderId(query.leaderId());
    }

    @Override
    public Optional<Group> handle(GetGroupByCodeQuery query) {
        var code = new GroupCode(query.code());
        var group = this.groupRepository.findByCode(code);
        if (group.isEmpty()) {
            return Optional.empty();
        }
        return group;
    }

    @Override
    public List<Group> handle(GetGroupsByUserIdQuery query) {
        var groupProfiles = this.groupProfileRepository.findByUser_Id(query.userId());
        List<Group> groups = new java.util.ArrayList<>(List.of());
        groupProfiles.forEach(groupProfile -> {
            groups.add(groupProfile.getGroup());
        });
        return groups;
    }

    @Override
    public Optional<Group> handle(GetGroupByIdQuery query) {
        return this.groupRepository.findById(query.groupId());
    }



}
