package com.hub.synhub_backend.groups.application.internal.acl;

import com.hub.synhub_backend.groups.domain.exceptions.InvalidActiveGroupSelectionException;
import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.groups.domain.model.queries.GetGroupByIdQuery;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.groups.domain.services.GroupQueryService;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.groups.interfaces.acl.GroupsContextFacade;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupsContextFacadeImpl implements GroupsContextFacade {

    private final GroupQueryService groupQueryService;
    private final GroupProfileRepository groupProfileRepository;
    private final GroupRepository groupRepository;

    /**
     * Usa los repositorios de su propio contexto y no {@code GroupCommandService}: este depende de
     * {@code TasksContextFacade} y tasks depende de esta fachada, así que pasar por el servicio formaría un ciclo de beans.
     */
    public GroupsContextFacadeImpl(
            GroupQueryService groupQueryService,
            GroupProfileRepository groupProfileRepository,
            GroupRepository groupRepository) {
        this.groupQueryService = groupQueryService;
        this.groupProfileRepository = groupProfileRepository;
        this.groupRepository = groupRepository;
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

    @Override
    public List<Group> fetchLedGroups(Long userId) {
        return groupProfileRepository.findByUser_IdAndRoleInGroup(userId, GroupRole.GROUP_LEADER).stream()
                .map(GroupProfile::getGroup)
                .sorted(Comparator.comparing(Group::getId))
                .toList();
    }

    @Override
    @Transactional
    public void applyGroupLimit(Long userId, int limit, List<Long> activeGroupIds) {
        var active = fetchLedGroups(userId).stream()
                .filter(group -> !group.isLocked())
                .toList();
        if (active.size() <= limit) return;

        var chosen = activeGroupIds == null ? List.<Long>of() : activeGroupIds;
        var activeIds = active.stream().map(Group::getId).toList();
        if (chosen.size() != limit || chosen.stream().distinct().count() != limit || !activeIds.containsAll(chosen)) {
            throw new InvalidActiveGroupSelectionException(limit);
        }
        active.stream().filter(group -> !chosen.contains(group.getId())).forEach(group -> {
            group.lock();
            groupRepository.save(group);
        });
    }
}
