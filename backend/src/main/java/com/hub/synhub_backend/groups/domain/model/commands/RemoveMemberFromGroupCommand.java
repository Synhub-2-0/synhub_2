package com.hub.synhub_backend.groups.domain.model.commands;

public record RemoveMemberFromGroupCommand(Long leaderId, Long memberId) {
}
