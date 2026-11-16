package com.hub.synhub_backend.tasks.domain.model.valueobjects;

/** Clasificación que reemplaza a la guardada al editar una tarea: un campo nulo o vacío la quita. */
public record TaskClassificationUpdate(
        Integer difficulty, String context, String urgency, String labels, Boolean aiAccepted) {}
