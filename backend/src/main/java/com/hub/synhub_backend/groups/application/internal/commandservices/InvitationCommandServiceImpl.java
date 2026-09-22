package com.hub.synhub_backend.groups.application.internal.commandservices;

import com.hub.synhub_backend.groups.domain.exceptions.UserAlreadyHasInvitationForGroupException;
import com.hub.synhub_backend.groups.domain.exceptions.UserAlreadyInGroupException;
import com.hub.synhub_backend.groups.domain.model.aggregates.Invitation;
import com.hub.synhub_backend.groups.domain.model.commands.AcceptInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.commands.CancelInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.commands.CreateInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.groups.domain.services.InvitationCommandService;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.InvitationRepository;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class InvitationCommandServiceImpl implements InvitationCommandService {

    private final InvitationRepository invitationRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupProfileRepository groupProfileRepository;

    public InvitationCommandServiceImpl(
            InvitationRepository invitationRepository,
            GroupRepository groupRepository,
            UserRepository userRepository,
            GroupProfileRepository groupProfileRepository
    ) {
        this.invitationRepository = invitationRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.groupProfileRepository = groupProfileRepository;
    }

    @Override
    @Transactional
    public Invitation handle(CreateInvitationCommand command) {
        var user = this.userRepository.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Member with id " + command.userId() + " does not exist"));
        var group = this.groupRepository.findById(command.groupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group with id " + command.groupId() + " does not exist"));
        if (!group.getLeader().getId().equals(command.requesterId())) {
            throw new AccessDeniedException("User with id " + command.requesterId() + " is not the leader of the group");
        }
        if(this.invitationRepository.existsByUser_IdAndGroup_Id(command.userId(), command.groupId())) {
            throw new UserAlreadyHasInvitationForGroupException("User with id " + command.userId() + " already has an invitation for group with id " + command.groupId());
        }
        if (this.groupProfileRepository.existsByGroup_IdAndUser_Id(command.groupId(), command.userId())) {
            throw new UserAlreadyInGroupException("User with id " + command.userId() + " is already in group with id " + command.groupId());
        }
        var createdInvitation = new Invitation(
                user,
                group
        );
        this.invitationRepository.save(createdInvitation);
        return createdInvitation;
    }

    @Override
    @Transactional
    public void handle(CancelInvitationCommand command) {
        var invitation = this.invitationRepository.findById(command.invitationId())
                .orElseThrow(() -> new ResourceNotFoundException("Invitation with id " + command.invitationId() + " does not exist"));
        var requester = this.userRepository.findById(command.requesterId())
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + command.requesterId() + " does not exist"));

        if (!(invitation.getUser().getId().equals(requester.getId()) || invitation.getUser().getId().equals(invitation.getGroup().getLeader().getId()))) {
            throw new AccessDeniedException("Member with id " + command.requesterId() + " is not the receiver nor the owner of the invitation");
        }
        this.invitationRepository.delete(invitation);
    }

    @Override
    @Transactional
    public void handle(AcceptInvitationCommand command) {
        var invitation = this.invitationRepository.findById(command.invitationId())
                .orElseThrow(() -> new ResourceNotFoundException("Invitation with id " + command.invitationId() + " does not exist"));
        var userToJoin = invitation.getUser();
        if (!Objects.equals(command.userId(), userToJoin.getId())) {
            throw new AccessDeniedException("User with id " + command.userId() + " is not the receiver of the invitation.");
        }
        var group = invitation.getGroup();
        if (this.groupProfileRepository.existsByGroup_IdAndUser_Id(group.getId(), userToJoin.getId())) {
            throw new UserAlreadyInGroupException("User with id " + userToJoin.getId() + " is already in group with id " + group.getId());
        }
        var newGroupProfile = new GroupProfile(userToJoin, group, GroupRole.GROUP_MEMBER);
        this.groupProfileRepository.save(newGroupProfile);
        this.invitationRepository.delete(invitation);
    }
}

