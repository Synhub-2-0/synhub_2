package com.hub.synhub_backend.groups.domain.model.commands;

/** Activar un grupo bloqueado; solo su líder y solo si su plan tiene un cupo libre. */
public record ActivateGroupCommand(Long requesterId, Long groupId) {}
