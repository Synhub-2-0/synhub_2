package com.hub.synhub_backend.tasks.domain.services;

import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.queries.*;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskQueryService {

    /** Una tarea por id; solo para integrantes de su grupo (AUDIT SEC-01). */
    Optional<Task> handle(GetTaskByIdQuery query);

    /** Tareas asignadas a un usuario en todos sus grupos, por fecha límite (GET /tasks/user). */
    List<Task> handle(GetAllTasksByUserId query);

    /** Todas las tareas de un grupo, sin control de acceso: uso interno (informe de IA). */
    List<Task> handle(GetAllTasksByGroupIdQuery query);

    /** Tareas de un grupo, opcionalmente por estado; solo para integrantes. */
    List<Task> handle(GetTasksByGroupIdAndStatus query);

    /** Página de tareas del grupo, ordenadas por fecha límite (AUDIT PERF-02). */
    Page<Task> handle(GetTasksByGroupIdAndStatus query, Pageable pageable);

    /** Tareas de un integrante dentro de un grupo; solo para integrantes. */
    List<Task> handle(GetTasksByGroupIdAndUserIdQuery query);
}
