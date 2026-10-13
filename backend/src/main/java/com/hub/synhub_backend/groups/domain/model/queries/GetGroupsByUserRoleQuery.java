package com.hub.synhub_backend.groups.domain.model.queries;

import com.hub.synhub_backend.groups.domain.model.valueobjects.GroupRole;

public record GetGroupsByUserRoleQuery(Long requesterId, GroupRole role) {
}
