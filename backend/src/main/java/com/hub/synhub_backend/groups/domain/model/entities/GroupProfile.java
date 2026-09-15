package com.hub.synhub_backend.groups.domain.model.entities;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.iam.domain.model.entities.Role;
import com.hub.synhub_backend.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class GroupProfile extends AuditableModel {

    @ManyToOne()
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
}
