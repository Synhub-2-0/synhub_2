package com.hub.synhub_backend.groups.domain.model.entities;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.domain.model.entities.Role;
import com.hub.synhub_backend.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Entity
public class GroupProfile extends AuditableModel {

    @ManyToOne()
    @JoinColumn(name = "user_id")
    @Getter
    private User user;

    @ManyToOne
    @JoinColumn(name = "group_id")
    @Getter
    private Group group;

    @ManyToOne
    @JoinColumn(name = "role_id")
    @Getter
    private Role role;

    public GroupProfile() {
        this.user = null;
        this.group = null;
        this.role = null;
    }

    public GroupProfile(User user, Group group, Role role) {
        this.user = user;
        this.group = group;
        this.role = role;
    }
}
