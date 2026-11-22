package com.hub.synhub_backend.subscriptions.domain.model.commands;

import java.util.List;

/** Cambiar el plan del propio usuario. {@code activeGroupIds}: los grupos que conserva activos si el nuevo límite no le alcanza. */
public record ChangePlanCommand(Long userId, String plan, List<Long> activeGroupIds) {}
