package com.hub.synhub_backend.groups.domain.model.aggregates;

import com.hub.synhub_backend.groups.domain.exceptions.GroupLockedException;
import com.hub.synhub_backend.groups.domain.model.commands.UpdateGroupCommand;
import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupCode;
import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;
import com.hub.synhub_backend.groups.domain.model.valueobjects.ImgUrl;
import com.hub.synhub_backend.iam.domain.model.aggregates.User;
import com.hub.synhub_backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

/**
 * Borrado lógico (decisión HUM-03): {@code delete} marca {@code deleted_at} y Hibernate excluye la fila
 * de todas las consultas, cargas y colecciones. Los datos siguen en la base.
 */
@Entity
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
@NoArgsConstructor
@Setter
@Getter
public class Group extends AuditableAbstractAggregateRoot<Group> {

    @Embedded
    @AttributeOverride(name = "code", column = @Column(name = "code", unique = true))
    private GroupCode code;

    @NotNull
    private String name;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String description;

    @Embedded
    private ImgUrl imgUrl;

    /** Bloqueado: el líder superó el límite de su plan. Es de solo lectura hasta que se active. */
    @Column(nullable = false)
    private boolean locked = false;

    /**
     * Membresías. Se cargan siempre (EAGER): el líder y la cantidad de integrantes forman parte de cualquier
     * representación del grupo, también fuera de una transacción (sin OSIV, AUDIT ARC-04). La carga por lotes
     * (default_batch_fetch_size) evita una consulta por grupo.
     */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<GroupProfile> userProfiles = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<Invitation> invitations = new ArrayList<>();

    public Group(String name, String description, String imgUrl, GroupCode code) {
        this.name = name;
        this.imgUrl = new ImgUrl(imgUrl);
        this.description = description;
        this.code = code;
    }

    public int getMemberCount() {
        return this.userProfiles.size();
    }

    public User getLeader() {
        GroupProfile foundLeaderProfile = this.userProfiles.stream()
                .filter(profile -> profile.getRoleInGroup() == GroupRole.GROUP_LEADER)
                .findFirst()
                .orElse(null);
        if (foundLeaderProfile != null) {
            return foundLeaderProfile.getUser();
        }

        return null;
    }

    public void lock() {
        this.locked = true;
    }

    public void unlock() {
        this.locked = false;
    }

    /** Todo lo que un usuario hace en el grupo (tareas, solicitudes, invitaciones, edición) exige que esté activo. */
    public void ensureNotLocked() {
        if (this.locked) throw new GroupLockedException(this.getId());
    }

    public void updateInformation(UpdateGroupCommand command) {
        // Un campo ausente (null) o vacío no cambia.
        this.name = isBlank(command.name()) ? this.name : command.name();
        this.description = isBlank(command.description()) ? this.description : command.description();
        this.imgUrl = isBlank(command.imgUrl()) ? this.imgUrl : new ImgUrl(command.imgUrl());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
