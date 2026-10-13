package com.hub.synhub_backend.groups;

import com.hub.synhub_backend.groups.application.internal.commandservices.InvitationCommandServiceImpl;
import com.hub.synhub_backend.groups.domain.exceptions.UserAlreadyHasInvitationForGroupException;
import com.hub.synhub_backend.groups.domain.exceptions.UserAlreadyInGroupException;
import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.aggregates.Invitation;
import com.hub.synhub_backend.groups.domain.model.commands.AcceptInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.commands.CancelInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.commands.CreateInvitationCommand;
import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.InvitationRepository;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.interfaces.acl.IamContextFacade;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static com.hub.synhub_backend.groups.GroupFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Reglas de invitación: crear, aceptar y cancelar. */
class InvitationCommandServiceImplTest {

    private static final long LEADER = 1, MEMBER = 2, GUEST = 3, STRANGER = 4, GROUP = 10, INVITATION = 50;

    private InvitationRepository invitations;
    private GroupRepository groups;
    private IamContextFacade users;
    private GroupProfileRepository profiles;
    private InvitationCommandServiceImpl service;
    private User guest;
    private Group group;
    private Invitation invitation;

    @BeforeEach
    void setUp() {
        invitations = mock(InvitationRepository.class);
        groups = mock(GroupRepository.class);
        users = mock(IamContextFacade.class);
        profiles = mock(GroupProfileRepository.class);
        service = new InvitationCommandServiceImpl(invitations, groups, users, profiles);

        guest = user(GUEST, "invitado");
        group = group(GROUP, user(LEADER, "lider"), user(MEMBER, "ana"));
        invitation = invitation(INVITATION, guest, group);
        when(users.fetchUserById(GUEST)).thenReturn(Optional.of(guest));
        when(users.fetchUserById(LEADER)).thenReturn(Optional.of(group.getLeader()));
        when(users.fetchUserById(STRANGER)).thenReturn(Optional.of(user(STRANGER, "ajeno")));
        when(groups.findById(GROUP)).thenReturn(Optional.of(group));
        when(invitations.findById(INVITATION)).thenReturn(Optional.of(invitation));
    }

    // ---- Crear
    @Test void leaderInvitesUser() {
        var created = service.handle(new CreateInvitationCommand(LEADER, GUEST, GROUP));
        assertSame(guest, created.getUser());
        assertSame(group, created.getGroup());
        verify(invitations).save(created);
    }

    @Test void onlyLeaderCanInvite() {
        assertThrows(AccessDeniedException.class, () -> service.handle(new CreateInvitationCommand(MEMBER, GUEST, GROUP)));
        verify(invitations, never()).save(any());
    }

    @Test void duplicateInvitationIsRejected() {
        when(invitations.existsByUser_IdAndGroup_Id(GUEST, GROUP)).thenReturn(true);
        assertThrows(UserAlreadyHasInvitationForGroupException.class,
                () -> service.handle(new CreateInvitationCommand(LEADER, GUEST, GROUP)));
    }

    @Test void existingMemberCannotBeInvited() {
        when(profiles.existsByGroup_IdAndUser_Id(GROUP, GUEST)).thenReturn(true);
        assertThrows(UserAlreadyInGroupException.class,
                () -> service.handle(new CreateInvitationCommand(LEADER, GUEST, GROUP)));
    }

    @Test void unknownUserOrGroupIs404() {
        when(users.fetchUserById(99L)).thenReturn(Optional.empty());
        when(groups.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new CreateInvitationCommand(LEADER, 99L, GROUP)));
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new CreateInvitationCommand(LEADER, GUEST, 99L)));
    }

    // ---- Aceptar
    @Test void acceptingCreatesMembershipAndDeletesInvitation() {
        service.handle(new AcceptInvitationCommand(GUEST, INVITATION));

        var membership = ArgumentCaptor.forClass(GroupProfile.class);
        verify(profiles).save(membership.capture());
        assertSame(guest, membership.getValue().getUser());
        assertSame(group, membership.getValue().getGroup());
        assertEquals(GroupRole.GROUP_MEMBER, membership.getValue().getRoleInGroup());
        verify(invitations).delete(invitation);
    }

    @Test void onlyTheInvitedUserCanAccept() {
        assertThrows(AccessDeniedException.class, () -> service.handle(new AcceptInvitationCommand(STRANGER, INVITATION)));
        verify(profiles, never()).save(any());
    }

    @Test void acceptingWhenAlreadyMemberIsRejected() {
        when(profiles.existsByGroup_IdAndUser_Id(GROUP, GUEST)).thenReturn(true);
        assertThrows(UserAlreadyInGroupException.class, () -> service.handle(new AcceptInvitationCommand(GUEST, INVITATION)));
    }

    @Test void acceptingUnknownInvitationIs404() {
        when(invitations.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new AcceptInvitationCommand(GUEST, 99L)));
    }

    // ---- Cancelar / rechazar
    @Test void invitedUserDeclines() {
        service.handle(new CancelInvitationCommand(GUEST, INVITATION));
        verify(invitations).delete(invitation);
    }

    @Test void leaderCancels() {
        service.handle(new CancelInvitationCommand(LEADER, INVITATION));
        verify(invitations).delete(invitation);
    }

    @Test void strangerCannotCancel() {
        assertThrows(AccessDeniedException.class, () -> service.handle(new CancelInvitationCommand(STRANGER, INVITATION)));
        verify(invitations, never()).delete(any());
    }
}
