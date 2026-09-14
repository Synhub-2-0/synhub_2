package com.hub.synhub_backend.groups.application.internal.commandservices;

import com.hub.synhub_backend.groups.domain.model.aggregates.Invitation;
import com.hub.synhub_backend.groups.domain.model.commands.AcceptInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.commands.CancelInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.commands.CreateInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.commands.RejectInvitationCommand;
import com.hub.synhub_backend.groups.domain.services.InvitationCommandService;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.InvitationRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.LeaderRepository;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class InvitationCommandServiceImpl implements InvitationCommandService {

    private final InvitationRepository invitationRepository;
    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;
    private final LeaderRepository leaderRepository;

    public InvitationCommandServiceImpl(InvitationRepository invitationRepository, GroupRepository groupRepository, MemberRepository memberRepository, LeaderRepository leaderRepository) {
        this.invitationRepository = invitationRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.leaderRepository = leaderRepository;
    }

    @Override
    public Optional<Invitation> handle(CreateInvitationCommand command) {
        var member = this.memberRepository.findById(command.memberId());
        if (member.isEmpty()) {
            throw new IllegalArgumentException("Member with id " + command.memberId() + " does not exist");
        }
        var group = this.groupRepository.findById(command.groupId());
        if (group.isEmpty()) {
            throw new IllegalArgumentException("Group with id " + command.groupId() + " does not exist");
        }

        if(this.invitationRepository.existsByMember_Id(command.memberId())) {
            throw new IllegalArgumentException("Member with id " + command.memberId() + " already has an invitation");
        }

        var createdInvitation = new Invitation(
                member.get(),
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
        var member = this.memberRepository.findById(command.memberId());
        if (member.isEmpty()) {
            throw new IllegalArgumentException("Member with id " + command.memberId() + " does not exist");
        }
        if (!invitation.get().getMember().getId().equals(member.get().getId())) {
            throw new IllegalArgumentException("Member with id " + command.memberId() + " is not the owner of the invitation");
        }
        this.invitationRepository.delete(invitation.get());
    }

    private Invitation validateAndGetInvitation(Long invitationId, Long leaderId) {
        var invitation = this.invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation with id " + invitationId + " does not exist"));
        var leader = this.leaderRepository.findById(leaderId)
                .orElseThrow(() -> new IllegalArgumentException("Leader with id " + leaderId + " does not exist"));
        if (!invitation.getGroup().getLeader().getId().equals(leader.getId())) {
            throw new IllegalArgumentException("Leader with id " + leaderId + " is not the owner of the invitation");
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

        var member = invitation.getMember();
        var group = invitation.getGroup();

        member.setGroup(group);
        group.getMembers().add(member);
        group.setMemberCount(group.getMembers().size());
        this.memberRepository.save(member);
        this.groupRepository.save(group);

        this.invitationRepository.delete(invitation);
    }
}
