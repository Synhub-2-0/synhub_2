package com.hub.synhub_backend.groups.domain.model.aggregates;

import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

/**
 * Borrado lógico (decisión HUM-03): {@code delete} marca {@code deleted_at} y Hibernate excluye la fila
 * de todas las consultas, cargas y colecciones. Los datos siguen en la base.
 */
@Entity
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
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
