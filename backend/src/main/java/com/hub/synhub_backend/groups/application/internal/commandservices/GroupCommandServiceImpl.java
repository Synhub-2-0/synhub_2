package com.hub.synhub_backend.groups.application.internal.commandservices;

import com.hub.synhub_backend.groups.domain.exceptions.LeaderCannotBeRemovedException;
import com.hub.synhub_backend.groups.domain.exceptions.UserAlreadyInGroupException;
import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.commands.*;
import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.groups.domain.exceptions.UserNotInGroupException;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupCode;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.groups.domain.services.GroupCommandService;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.InvitationRepository;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.interfaces.acl.TasksContextFacade;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GroupCommandServiceImpl implements GroupCommandService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupProfileRepository groupProfileRepository;
    private final TasksContextFacade tasksContextFacade;
    private final InvitationRepository invitationRepository;

    public GroupCommandServiceImpl(
            GroupRepository groupRepository,
            UserRepository userRepository,
            GroupProfileRepository groupProfileRepository,
            TasksContextFacade tasksContextFacade,
            InvitationRepository invitationRepository
    ) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.groupProfileRepository = groupProfileRepository;
        this.tasksContextFacade = tasksContextFacade;
        this.invitationRepository = invitationRepository;
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

        // Soft delete (HUM-03): primero las tareas y sus requests; membresías e invitaciones caen en cascada
        // con el grupo (AUDIT BUG-02). Todo queda marcado con deleted_at y deja de ser visible.
        tasksContextFacade.deleteAllTasksByGroupId(group.getId());
        groupRepository.delete(group);

    }

    @Override
    @Transactional
    public void handle(RemoveMemberUserFromGroupCommand command) {
        var group = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group with id " + command.groupId() + " does not exist"));

        var groupProfile = this.groupProfileRepository.findByGroup_IdAndUser_Id(command.groupId(), command.userId())
                .orElseThrow(() -> new UserNotInGroupException("User with id " + command.userId() + " is not in group with id " + command.groupId()));

        var leader = group.getLeader();
        var isLeader = leader != null && leader.getId().equals(command.requesterId());
        var isSelf = command.userId().equals(command.requesterId());
        if (!isLeader && !isSelf) {
            throw new AccessDeniedException("Only the leader can remove other members from the group");
        }
        if (groupProfile.getRoleInGroup() == GroupRole.GROUP_LEADER) {
            throw new LeaderCannotBeRemovedException("The leader cannot be removed from the group");
        }

        // Soft delete (HUM-03): primero las tareas del integrante (y sus requests), después la membresía.
        tasksContextFacade.deleteTasksByUserIdAndGroupId(command.userId(), command.groupId());
        group.getUserProfiles().remove(groupProfile);
        groupProfile.getUser().getGroupProfiles().remove(groupProfile);
        groupProfileRepository.delete(groupProfile);
    }

    @Override
    @Transactional
    public Group handle(JoinGroupByCodeCommand command) {
        var group = groupRepository.findByCode(new GroupCode(command.code()))
                .orElseThrow(() -> new ResourceNotFoundException("Group with code " + command.code() + " does not exist"));
        var user = userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + command.userId() + " does not exist"));
        if (groupProfileRepository.existsByGroup_IdAndUser_Id(group.getId(), user.getId())) {
            throw new UserAlreadyInGroupException("User with id " + user.getId() + " is already in group with id " + group.getId());
        }

        var membership = new GroupProfile(user, group, GroupRole.GROUP_MEMBER);
        group.getUserProfiles().add(membership);
        user.getGroupProfiles().add(membership);
        groupProfileRepository.save(membership);
        // Una invitación pendiente al mismo grupo ya no tiene sentido.
        invitationRepository.deleteAll(invitationRepository.findByUser_IdAndGroup_Id(user.getId(), group.getId()));
        return group;
    }
}
