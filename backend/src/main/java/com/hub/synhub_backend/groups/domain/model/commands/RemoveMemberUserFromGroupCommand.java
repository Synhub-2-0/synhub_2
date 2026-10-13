package com.hub.synhub_backend.groups.domain.model.commands;

/**
 * Quitar a un integrante del grupo (HUM-05). El líder expulsa a cualquier miembro; un miembro puede salir
 * por su cuenta ({@code requesterId == userId}). El líder no puede ser quitado.
 */
public record RemoveMemberUserFromGroupCommand(Long requesterId, Long userId, Long groupId) {
}
