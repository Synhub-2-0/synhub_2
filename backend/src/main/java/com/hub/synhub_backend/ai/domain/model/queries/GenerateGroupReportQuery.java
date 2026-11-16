package com.hub.synhub_backend.ai.domain.model.queries;

/** requesterId: quien pide el informe (el líder); con él se leen los integrantes del grupo. */
public record GenerateGroupReportQuery(Long groupId, Long requesterId) {}
