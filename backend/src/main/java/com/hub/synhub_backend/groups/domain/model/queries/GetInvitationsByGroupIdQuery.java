package com.hub.synhub_backend.groups.domain.model.queries;

/** Invitaciones pendientes de un grupo; solo las ve el líder. */
public record GetInvitationsByGroupIdQuery(Long groupId, Long requesterId) {
}
