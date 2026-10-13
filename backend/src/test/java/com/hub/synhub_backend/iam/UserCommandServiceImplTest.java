package com.hub.synhub_backend.iam;

import com.hub.synhub_backend.iam.application.internal.commandservices.UserCommandServiceImpl;
import com.hub.synhub_backend.iam.application.internal.outboundservices.hashing.HashingService;
import com.hub.synhub_backend.iam.application.internal.outboundservices.tokens.TokenService;
import com.hub.synhub_backend.iam.domain.exceptions.EmailAlreadyExistsException;
import com.hub.synhub_backend.iam.domain.exceptions.UsernameAlreadyExistsException;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.domain.model.commands.SignInCommand;
import com.hub.synhub_backend.iam.domain.model.commands.SignUpCommand;
import com.hub.synhub_backend.iam.domain.model.entities.Role;
import com.hub.synhub_backend.iam.domain.model.valueobjects.AccountRole;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Registro e inicio de sesión. */
class UserCommandServiceImplTest {

    private UserRepository users;
    private HashingService hashing;
    private TokenService tokens;
    private RoleRepository roles;
    private UserCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        hashing = mock(HashingService.class);
        tokens = mock(TokenService.class);
        roles = mock(RoleRepository.class);
        service = new UserCommandServiceImpl(users, hashing, tokens, roles);
        when(roles.findByName(AccountRole.ROLE_USER)).thenReturn(Optional.of(new Role(1L, AccountRole.ROLE_USER)));
    }

    private static User ana() {
        return new User("ana", "Ana", "Torres", "img", "ana@synhub.io", "hash");
    }

    private static SignUpCommand signUp(String username, String email, List<Role> requested) {
        return new SignUpCommand(username, "Ana", "Torres", "img", email, "Secret123!", requested);
    }

    // ---- Sign-in
    @Test void signInReturnsUserAndToken() {
        var user = ana();
        when(users.findByUsername("ana")).thenReturn(Optional.of(user));
        when(hashing.matches("Secret123!", "hash")).thenReturn(true);
        when(tokens.generateToken("ana")).thenReturn("jwt");

        var result = service.handle(new SignInCommand("ana", "Secret123!")).orElseThrow();

        assertSame(user, result.getLeft());
        assertEquals("jwt", result.getRight());
    }

    @Test void signInWithWrongPasswordFails() {
        when(users.findByUsername("ana")).thenReturn(Optional.of(ana()));
        when(hashing.matches(anyString(), anyString())).thenReturn(false);
        assertThrows(BadCredentialsException.class, () -> service.handle(new SignInCommand("ana", "bad")));
        verify(tokens, never()).generateToken(anyString());
    }

    @Test void signInWithUnknownUserFails() {
        when(users.findByUsername("nobody")).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class, () -> service.handle(new SignInCommand("nobody", "x")));
    }

    // ---- Sign-up
    @Test void signUpStoresHashedPasswordAndResolvedRoles() {
        when(hashing.encode("Secret123!")).thenReturn("hashed");
        when(users.findByUsername("ana")).thenReturn(Optional.of(ana()));

        var result = service.handle(signUp("ana", "ana@synhub.io", List.of(new Role(AccountRole.ROLE_USER))));

        assertTrue(result.isPresent());
        var saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertEquals("hashed", saved.getValue().getPassword());
        assertEquals(List.of(AccountRole.ROLE_USER),
                saved.getValue().getAccountRoles().stream().map(Role::getName).toList());
    }

    @Test void signUpWithoutRolesGetsDefaultRole() {
        when(hashing.encode(anyString())).thenReturn("hashed");
        service.handle(signUp("ana", "ana@synhub.io", List.of()));
        var saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertEquals(List.of(AccountRole.ROLE_USER),
                saved.getValue().getAccountRoles().stream().map(Role::getName).toList());
    }

    @Test void duplicateUsernameIsRejected() {
        when(users.existsByUsername("ana")).thenReturn(true);
        assertThrows(UsernameAlreadyExistsException.class,
                () -> service.handle(signUp("ana", "ana@synhub.io", List.of())));
        verify(users, never()).save(any());
    }

    @Test void unknownRoleIsRejectedAndNothingIsSaved() {
        when(roles.findByName(AccountRole.ROLE_ADMIN)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> service.handle(signUp("ana", "ana@synhub.io", List.of(new Role(AccountRole.ROLE_ADMIN)))));
        verify(users, never()).save(any());
    }

    @Disabled("AUDIT BUG-04: el sign-up compara el email con el username (existsByEmail(command.username()))")
    @Test void duplicateEmailIsRejected() {
        when(users.existsByEmail("ana@synhub.io")).thenReturn(true);
        assertThrows(EmailAlreadyExistsException.class,
                () -> service.handle(signUp("otra", "ana@synhub.io", List.of())));
        verify(users, never()).save(any());
    }
}
