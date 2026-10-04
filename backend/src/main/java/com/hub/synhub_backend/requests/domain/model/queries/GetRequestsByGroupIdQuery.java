package com.hub.synhub_backend.requests.domain.model.queries;

/** status y type son filtros opcionales (null = todos). */
public record GetRequestsByGroupIdQuery(Long groupId, String status, String type) {
}
