package com.hub.synhub_backend.groups.domain.model.commands;

public record UpdateGroupCommand(
        Long leaderId,
        String name,
        String description,
        String imgUrl
) {
}
