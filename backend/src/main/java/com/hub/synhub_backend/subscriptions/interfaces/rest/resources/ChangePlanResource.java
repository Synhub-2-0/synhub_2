package com.hub.synhub_backend.subscriptions.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Cambio de plan con pago simulado: no lleva ningún dato de pago. */
public record ChangePlanResource(@NotBlank String plan, List<Long> activeGroupIds) {}
