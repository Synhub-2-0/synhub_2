package com.hub.synhub_backend.requests.application.internal.acl;

import com.hub.synhub_backend.requests.infrastructure.persistence.jpa.repositories.RequestRepository;
import com.hub.synhub_backend.requests.interfaces.acl.RequestsContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Usa el repositorio de su propio contexto y no RequestCommandService: este depende de TasksContextFacade,
 * y tasks depende de esta fachada, así que pasar por el servicio formaría un ciclo de beans.
 */
@Service
public class RequestsContextFacadeImpl implements RequestsContextFacade {

    private final RequestRepository requestRepository;

    public RequestsContextFacadeImpl(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Override
    @Transactional
    public void deleteAllRequestsByTaskId(Long taskId) {
        requestRepository.deleteByTaskId(taskId);
    }
}
