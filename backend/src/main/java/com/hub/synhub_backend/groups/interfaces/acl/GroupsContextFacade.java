package com.hub.synhub_backend.groups.interfaces.acl;

import java.util.Optional;

public interface GroupsContextFacade {
    /** Id del líder del grupo; vacío si el grupo no existe. */
    Optional<Long> fetchLeaderIdByGroupId(Long groupId);

    /** true si el usuario pertenece al grupo con cualquier rol (el líder incluido). */
    boolean isMember(Long groupId, Long userId);

    /** true si el usuario es el líder del grupo. */
    boolean isLeader(Long groupId, Long userId);
}
