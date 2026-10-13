package com.hub.synhub_backend.iam;

import com.hub.synhub_backend.iam.application.internal.commandservices.RoleCommandServiceImpl;
import com.hub.synhub_backend.iam.domain.model.commands.SeedRolesCommand;
import com.hub.synhub_backend.iam.domain.model.entities.Role;
import com.hub.synhub_backend.iam.domain.model.valueobjects.AccountRole;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.RoleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Roles de cuenta y su seed. */
class RoleTest {

    @Test void emptyOrNullRoleListFallsBackToRoleUser() {
        assertEquals(AccountRole.ROLE_USER, Role.validateRoleSet(null).getFirst().getName());
        assertEquals(AccountRole.ROLE_USER, Role.validateRoleSet(List.of()).getFirst().getName());
    }

    @Test void givenRolesAreKept() {
        var admin = new Role(AccountRole.ROLE_ADMIN);
        assertEquals(List.of(admin), Role.validateRoleSet(List.of(admin)));
    }

    @Test void roleFromName() {
        assertEquals(AccountRole.ROLE_ADMIN, Role.toRoleFromName("ROLE_ADMIN").getName());
        assertEquals("ROLE_ADMIN", Role.toRoleFromName("ROLE_ADMIN").getStringName());
        assertThrows(IllegalArgumentException.class, () -> Role.toRoleFromName("ROLE_ROOT"));
    }

    @Test void seedCreatesOnlyMissingRoles() {
        var repo = mock(RoleRepository.class);
        when(repo.existsByName(AccountRole.ROLE_USER)).thenReturn(true);
        when(repo.existsByName(AccountRole.ROLE_ADMIN)).thenReturn(false);

        new RoleCommandServiceImpl(repo).handle(new SeedRolesCommand());

        var saved = ArgumentCaptor.forClass(Role.class);
        verify(repo, times(1)).save(saved.capture());
        assertEquals(AccountRole.ROLE_ADMIN, saved.getValue().getName());
    }

    // AUDIT BUG-13: la igualdad depende del nombre, no del id que asigna la base
    @Test void rolesAreEqualByName() {
        var saved = new Role(1L, AccountRole.ROLE_USER);
        var fresh = new Role(AccountRole.ROLE_USER);
        assertEquals(saved, fresh);
        assertEquals(saved.hashCode(), fresh.hashCode());
        assertNotEquals(saved, new Role(AccountRole.ROLE_ADMIN));
        var set = new java.util.HashSet<Role>(java.util.List.of(fresh));
        fresh.setId(9L);
        assertTrue(set.contains(saved), "asignar el id no cambia el hash dentro del Set");
    }
}
