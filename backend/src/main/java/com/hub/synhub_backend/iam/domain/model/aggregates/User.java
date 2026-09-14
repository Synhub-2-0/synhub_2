package com.hub.synhub_backend.iam.domain.model.aggregates;

import io.micrometer.common.lang.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import com.hub.synhub_backend.groups.domain.model.aggregates.Leader;
import com.hub.synhub_backend.iam.domain.model.entities.Role;
import com.hub.synhub_backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Member;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
public class User extends AuditableAbstractAggregateRoot<User> {

    @NotBlank
    @Size(max = 50)
    @Column(unique = true)
    private String username;

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotBlank
    @Size(max = 50)
    private String surname;

    @NotBlank
    @Column(length = 1024)
    private String imgUrl;

    @NotBlank
    @Column(unique = true)
    private String email;

    @NotBlank
    @Size(max = 120)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

    @Nullable
    @Setter
    @OneToOne
    @JoinColumn(name = "leader_id")
    private Leader leader;

    @Nullable
    @Setter
    @OneToOne
    @JoinColumn(name = "member_id")
    private Member member;

    public User() {
        this.roles = new HashSet<>();
    }
    public User(String username,
                String name,
                String surname,
                String imgUrl,
                String email,
                String password) {
        this.username = username;
        this.name = name;
        this.surname = surname;
        this.imgUrl = imgUrl;
        this.email = email;
        this.password = password;
        this.roles = new HashSet<>();
    }

    public User(
            String username,
            String name,
            String surname,
            String imgUrl,
            String email,
            String password,
            List<Role> roles) {
        this(
                username,
                name,
                surname,
                imgUrl,
                email,
                password);
        addRoles(roles);
    }


    public User addRole(Role role) {
        this.roles.add(role);
        return this;
    }


    public User addRoles(List<Role> roles) {
        var validatedRoleSet = Role.validateRoleSet(roles);
        this.roles.addAll(validatedRoleSet);
        return this;
    }
}
