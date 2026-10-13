package com.hub.synhub_backend.groups.domain.model.queries;

/** Integrantes de un grupo; solo los puede ver alguien del grupo. */
public record GetGroupMembersByGroupIdQuery(Long groupId, Long requesterId) {}
