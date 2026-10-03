package com.hub.synhub_backend.groups.domain.model.commands;

public record UpdateGroupCommand(
        Long requesterId,
        Long groupId,
        String name,
        String description,
        String imgUrl
) {
}
