package com.hub.synhub_backend.groups.application.internal.commandservices;

import com.hub.synhub_backend.groups.domain.model.aggregates.Invitation;
import com.hub.synhub_backend.groups.domain.model.commands.AcceptInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.commands.CancelInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.commands.CreateInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.commands.RejectInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.groups.domain.services.InvitationCommandService;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.InvitationRepository;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
    public Optional<Invitation> handle(CreateInvitationCommand command) {
        var user = this.userRepository.findById(command.userId());
        if (user.isEmpty()) {
            throw new IllegalArgumentException("Member with id " + command.userId() + " does not exist");
        }
        var group = this.groupRepository.findById(command.groupId());
        if (group.isEmpty()) {
            throw new IllegalArgumentException("Group with id " + command.groupId() + " does not exist");
        }

        if(this.invitationRepository.existsByUser_IdAndGroup_Id(command.userId(), command.groupId())) {
            throw new IllegalArgumentException("User with id " + command.userId() + " already has an invitation for group with id " + command.groupId());
        }

        var createdInvitation = new Invitation(
                user.get(),
                group.get()
        );

        this.invitationRepository.save(createdInvitation);

        return Optional.of(createdInvitation);
    }

    @Override
    public void handle(CancelInvitationCommand command) {
        var invitation = this.invitationRepository.findById(command.invitationId());
        if (invitation.isEmpty()) {
            throw new IllegalArgumentException("Invitation with id " + command.invitationId() + " does not exist");
        }
        var user = this.userRepository.findById(command.userId());
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User with id " + command.userId() + " does not exist");
        }
        if (!invitation.get().getUser().getId().equals(user.get().getId())) {
            throw new IllegalArgumentException("Member with id " + command.userId() + " is not the owner of the invitation");
        }
        this.invitationRepository.delete(invitation.get());
    }

    private Invitation validateAndGetInvitation(Long invitationId, Long leaderId) {
        var invitation = this.invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation with id " + invitationId + " does not exist"));
        var leader = this.userRepository.findById(leaderId)
                .orElseThrow(() -> new IllegalArgumentException("Leader with id " + leaderId + " does not exist"));
        if (!invitation.getGroup().getLeader().getId().equals(leader.getId())) {
            throw new IllegalArgumentException("User with id " + leaderId + " is not the owner of the invitation");
        }
        return invitation;
    }

    @Override
    public void handle(RejectInvitationCommand command) {
        var invitation = validateAndGetInvitation(command.invitationId(), command.leaderId());
        this.invitationRepository.delete(invitation);
    }

    @Override
    public void handle(AcceptInvitationCommand command) {
        var invitation = validateAndGetInvitation(command.invitationId(), command.leaderId());

        var userToJoin = invitation.getUser();
        var group = invitation.getGroup();

        if (this.groupProfileRepository.existsByGroup_IdAndUser_Id(group.getId(), userToJoin.getId())) {
            throw new IllegalArgumentException("User with id " + userToJoin.getId() + " is already in group with id " + group.getId());
        }

        var newGroupProfile = new GroupProfile(userToJoin, group, GroupRole.GROUP_MEMBER);

        userToJoin.getGroupProfiles().add(newGroupProfile);
        group.getUserProfiles().add(newGroupProfile);
        group.setMemberCount(group.getUserProfiles().size());
        this.userRepository.save(userToJoin);
        this.groupRepository.save(group);
        this.groupProfileRepository.save(newGroupProfile);

        this.invitationRepository.delete(invitation);
    }
}
