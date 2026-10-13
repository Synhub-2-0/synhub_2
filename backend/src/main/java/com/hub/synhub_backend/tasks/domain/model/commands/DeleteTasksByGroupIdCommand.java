package com.hub.synhub_backend.tasks.domain.model.commands;

/** Borra todas las tareas de un grupo, con sus requests. Lo usa groups antes de borrar el grupo. */
public record DeleteTasksByGroupIdCommand(Long groupId) {}
