package com.hub.synhub_backend.groups.domain.model.aggregates;

import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import com.hub.synhub_backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;

@Entity
@Setter
@Getter
public class Invitation extends AuditableAbstractAggregateRoot<Invitation> {
    @ManyToOne
    @JoinColumn(name = "member_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    public Invitation() {}

    public Invitation(User user, Group group) {
        this.user = user;
        this.group = group;
    }
}
