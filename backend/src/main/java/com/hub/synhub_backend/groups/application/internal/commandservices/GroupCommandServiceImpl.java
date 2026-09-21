package com.hub.synhub_backend.groups.application.internal.commandservices;

import com.hub.synhub_backend.groups.domain.exceptions.LeaderCannotBeRemovedException;
import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.commands.*;
import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.groups.domain.exceptions.UserNotInGroupException;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupCode;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.groups.domain.services.GroupCommandService;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupCommandServiceImpl implements GroupCommandService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupProfileRepository groupProfileRepository;

    public GroupCommandServiceImpl(
            GroupRepository groupRepository,
            UserRepository userRepository,
            GroupProfileRepository groupProfileRepository
    ) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.groupProfileRepository = groupProfileRepository;
    }

    @Override
    @Transactional
    public Group handle(CreateGroupCommand command) {
        var leaderOpt = userRepository.findById(command.leaderId());
        if (leaderOpt.isEmpty()) {
            throw new ResourceNotFoundException("User with id " + command.leaderId() + " does not exist");
        }
        var leader = leaderOpt.get();
        Group group = new Group(
                command.name(),
                command.description(),
                command.imgUrl(),
                GroupCode.random()
        );
        while(groupRepository.existsByCode(group.getCode())) {
            group.setCode(GroupCode.random());
        }
        var newGroupProfile = new GroupProfile(leader, group, GroupRole.GROUP_LEADER);
        leader.getGroupProfiles().add(newGroupProfile);
        group.getUserProfiles().add(newGroupProfile);
        this.groupRepository.save(group);
        this.userRepository.save(leader);
        return group;
    }

    @Override
    @Transactional
    public Group handle(UpdateGroupCommand command) {
        var group = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group with id " + command.groupId() + " does not exist"));

        var leader = group.getLeader();
        if (leader == null || !leader.getId().equals(command.requesterId())) {
            throw new AccessDeniedException("Only the leader can update the group");
        }

        group.updateInformation(command);
        return groupRepository.save(group);
    }

    @Override
    @Transactional
    public void handle(DeleteGroupCommand command) {
        var groupOpt = groupRepository.findById(command.groupId());

        if (groupOpt.isEmpty()) {
            throw new ResourceNotFoundException("Group with id " + command.groupId() + " does not exist");
        }

        var group = groupOpt.get();
        var leader = group.getLeader();

        if (leader == null || !leader.getId().equals(command.requesterId())) {
            throw new AccessDeniedException("Only the leader of a group can delete it");
        }

        groupRepository.delete(group);

    }

    @Override
    @Transactional
    public void handle(RemoveMemberUserFromGroupCommand command) {
        var group = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group with id " + command.groupId() + " does not exist"));

        var user = this.userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + command.userId() + " does not exist"));

        var groupProfile = this.groupProfileRepository.findByGroup_IdAndUser_Id(command.groupId(), command.userId())
                .orElseThrow(() -> new UserNotInGroupException("User with id " + command.userId() + " is not in group with id " + command.groupId()));

        if (groupProfile.getRoleInGroup() == GroupRole.GROUP_LEADER) {
            throw new LeaderCannotBeRemovedException("The leader cannot be removed from the group");
        }

        user.getGroupProfiles().remove(groupProfile);
        group.getUserProfiles().remove(groupProfile);
        userRepository.save(user);
        groupRepository.save(group);
    }
}
