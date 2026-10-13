package com.hub.synhub_backend.groups.domain.model.queries;

/** Grupo por id, visible solo para sus integrantes (a diferencia de GetGroupByIdQuery, de uso interno). */
public record GetGroupForMemberQuery(Long groupId, Long requesterId) {}
