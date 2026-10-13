package com.hub.synhub_backend.groups.interfaces.acl;

import java.util.Optional;

public interface GroupsContextFacade {
    /** Id del líder del grupo; vacío si el grupo no existe. */
    Optional<Long> fetchLeaderIdByGroupId(Long groupId);
}
