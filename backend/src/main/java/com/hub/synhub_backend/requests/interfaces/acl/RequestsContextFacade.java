package com.hub.synhub_backend.requests.interfaces.acl;

/** Contrato que requests ofrece a otros contextos. */
public interface RequestsContextFacade {
    /** Borra todas las requests de una tarea (se usa antes de borrar la tarea). */
    void deleteAllRequestsByTaskId(Long taskId);
}
