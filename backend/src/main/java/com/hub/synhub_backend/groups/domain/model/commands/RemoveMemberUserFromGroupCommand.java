package com.hub.synhub_backend.groups.domain.model.commands;

public record RemoveMemberUserFromGroupCommand(Long userId, Long groupId) {
}
