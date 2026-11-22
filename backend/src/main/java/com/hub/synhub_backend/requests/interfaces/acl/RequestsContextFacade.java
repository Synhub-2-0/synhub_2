package com.hub.synhub_backend.requests.interfaces.acl;

/** Contrato que requests ofrece a otros contextos. */
public interface RequestsContextFacade {
    /** Borra todas las requests de una tarea (se usa antes de borrar la tarea). */
    void deleteAllRequestsByTaskId(Long taskId);

    /** ¿La tarea tiene una solicitud de modificación pendiente? Mientras la tenga, no se reanuda a mano. */
    boolean hasPendingModification(Long taskId);

    /** Cierra (aprobada) el aviso de vencimiento pendiente de la tarea: el líder la reanudó. Devuelve si había uno. */
    boolean resolvePendingExpiration(Long taskId);

    /** Marca como aprobada la modificación pendiente de la tarea (el líder la editó). Devuelve si había una. */
    boolean resolvePendingModification(Long taskId);
}
