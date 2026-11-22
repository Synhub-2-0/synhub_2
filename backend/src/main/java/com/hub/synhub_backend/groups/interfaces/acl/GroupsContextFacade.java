package com.hub.synhub_backend.groups.interfaces.acl;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import java.util.List;
import java.util.Optional;

public interface GroupsContextFacade {
    /** Id del líder del grupo; vacío si el grupo no existe. */
    Optional<Long> fetchLeaderIdByGroupId(Long groupId);

    /** true si el usuario pertenece al grupo con cualquier rol (el líder incluido). */
    boolean isMember(Long groupId, Long userId);

    /** true si el usuario es el líder del grupo. */
    boolean isLeader(Long groupId, Long userId);

    /** Membresía del usuario en el grupo (a ella se asignan las tareas); vacío si no pertenece. */
    Optional<GroupProfile> fetchMembership(Long groupId, Long userId);

    /** Grupos que el usuario lidera (activos y bloqueados), del más antiguo al más reciente. */
    List<Group> fetchLedGroups(Long userId);

    /**
     * Aplica el límite de grupos activos de un plan a los grupos que el usuario lidera. Si ya caben, no hace nada;
     * si no, {@code activeGroupIds} debe traer exactamente {@code limit} de sus grupos activos: el resto se bloquea.
     * Subir de plan no activa grupos: el líder los activa uno a uno.
     */
    void applyGroupLimit(Long userId, int limit, List<Long> activeGroupIds);
}
