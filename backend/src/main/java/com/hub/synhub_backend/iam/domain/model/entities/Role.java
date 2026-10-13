package com.hub.synhub_backend.iam.domain.model.entities;

import com.hub.synhub_backend.iam.domain.model.valueobjects.AccountRole;
import jakarta.persistence.*;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rol de cuenta. Vive dentro de {@code User.accountRoles} (un {@code Set}): la igualdad se basa en {@code name},
 * que es único y no cambia, en lugar del {@code id} que genera la base (AUDIT BUG-13).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AccountRole name;

    public Role(AccountRole name) {
        this.name = name;
    }

    public String getStringName() {
        return name.name();
    }

    public static Role getDefaultRole() {
        return new Role(AccountRole.ROLE_USER);
    }

    public static Role toRoleFromName(String name) {
        return new Role(AccountRole.valueOf(name));
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Role role && name == role.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return "Role(" + name + ")";
    }

    public static List<Role> validateRoleSet(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of(getDefaultRole());
        }
        return roles;
    }
}
