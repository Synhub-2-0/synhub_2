package com.hub.synhub_backend.groups.domain.model.commands;

public record CreateGroupCommand(
        String name,
        String imgUrl,
        String description,
        Long leaderId
) {
}
