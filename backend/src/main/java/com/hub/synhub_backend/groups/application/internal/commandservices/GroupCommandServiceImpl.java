package com.hub.synhub_backend.groups.application.internal.commandservices;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.commands.*;
import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
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

import java.util.Optional;

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
    public Optional<Group> handle(UpdateGroupCommand command) {
        var groupOpt = groupRepository.findById(command.groupId());

        if (groupOpt.isEmpty()) {
            throw new IllegalArgumentException("Group with id " + command.groupId() + "does not exist");
        }

        var groupToUpdate = groupOpt.get();
        groupToUpdate.updateInformation(command);

        try{
            var updatedGroup = groupRepository.save(groupToUpdate);
            return Optional.of(updatedGroup);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while updating group: " + e.getMessage());
        }
    }

    @Override
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
    public void handle(RemoveMemberUserFromGroupCommand command) {

        var groupOpt = groupRepository.findById(command.groupId());

        if (groupOpt.isEmpty()) {
            throw new IllegalArgumentException("Group with id " + command.groupId() + " does not exist");
        }

        var userOpt = this.userRepository.findById(command.userId());

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User with id " + command.userId() + " does not exist");
        }

        var group = groupOpt.get();
        var user = userOpt.get();

        var groupProfileOpt = this.groupProfileRepository.findByGroup_IdAndUser_Id(command.groupId(), command.userId());

        if (groupProfileOpt.isEmpty()) {
            throw new IllegalArgumentException("User with id " + command.userId() + " is not in group with id " + command.groupId());
        }

        var groupProfile = groupProfileOpt.get();

        if (groupProfile.getRoleInGroup() == GroupRole.GROUP_LEADER) {
            throw new IllegalArgumentException("The leader cannot be removed from the group");
        }

        try {
            user.getGroupProfiles().remove(groupProfile);
            group.getUserProfiles().remove(groupProfile);
            group.setMemberCount(group.getUserProfiles().size() + 1);
            userRepository.save(user);
            groupRepository.save(group);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while removing member from group: " + e.getMessage());
        }
    }
}
