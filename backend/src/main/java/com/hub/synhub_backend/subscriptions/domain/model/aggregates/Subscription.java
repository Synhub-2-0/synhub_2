package com.hub.synhub_backend.subscriptions.domain.model.aggregates;

import com.hub.synhub_backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.hub.synhub_backend.subscriptions.domain.model.valueobjects.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

/**
 * Plan de un usuario. Guarda solo el id del usuario (iam es otro contexto). Sin fila, el usuario está en Gratis.
 * Borrado lógico como el resto del modelo (HUM-03).
 */
@Getter
@Entity
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
@Table(name = "subscriptions")
public class Subscription extends AuditableAbstractAggregateRoot<Subscription> {
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan plan;

    protected Subscription() {}

    public Subscription(Long userId, SubscriptionPlan plan) {
        this.userId = userId;
        this.plan = plan;
    }

    public void changePlan(SubscriptionPlan plan) {
        this.plan = plan;
    }
}
