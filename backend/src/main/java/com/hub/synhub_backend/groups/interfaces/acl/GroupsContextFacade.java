package com.hub.synhub_backend.groups.interfaces.acl;

import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
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
}
