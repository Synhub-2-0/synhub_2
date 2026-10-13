package com.hub.synhub_backend.groups.domain.model.commands;

/** Unirse a un grupo con su código (HUM-05): el código funciona como invitación abierta. */
public record JoinGroupByCodeCommand(String code, Long userId) {
}
