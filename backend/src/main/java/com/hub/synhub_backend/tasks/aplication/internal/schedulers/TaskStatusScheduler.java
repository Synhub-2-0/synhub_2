package com.hub.synhub_backend.tasks.aplication.internal.schedulers;

import com.hub.synhub_backend.requests.domain.model.commands.CreateRequestCommand;
import com.hub.synhub_backend.requests.domain.services.RequestCommandService;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Vence las tareas {@code IN_PROGRESS} cuya fecha límite pasó y genera su request {@code EXPIRED}.
 * <p>
 * Seguro con varias instancias (AUDIT BUG-09): cada tarea se marca con un {@code UPDATE} condicional
 * ({@code WHERE status = IN_PROGRESS}) y solo la instancia que cambió la fila crea la request. Cada tarea va
 * en su propia transacción: si una falla, no deshace las demás.
 * La frecuencia se configura con {@code app.tasks.expiration-check-ms} (60 s por defecto).
 */
@Service
public class TaskStatusScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStatusScheduler.class);

    private final TaskRepository taskRepository;
    private final RequestCommandService requestCommandService;
    private final TransactionOperations transactions;

    @Autowired
    public TaskStatusScheduler(TaskRepository taskRepository, RequestCommandService requestCommandService,
                               PlatformTransactionManager transactionManager) {
        this(taskRepository, requestCommandService, new TransactionTemplate(transactionManager));
    }

    public TaskStatusScheduler(TaskRepository taskRepository, RequestCommandService requestCommandService,
                               TransactionOperations transactions) {
        this.taskRepository = taskRepository;
        this.requestCommandService = requestCommandService;
        this.transactions = transactions;
    }

    @Scheduled(fixedDelayString = "${app.tasks.expiration-check-ms:60000}",
               initialDelayString = "${app.tasks.expiration-check-ms:60000}")
    public void updateExpiredTasks() {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        for (var task : taskRepository.findAllByStatusAndDueDateBefore(TaskStatus.IN_PROGRESS, now)) {
            transactions.executeWithoutResult(status -> expire(task.getId(), now));
        }
    }

    private void expire(Long taskId, OffsetDateTime now) {
        if (taskRepository.markExpiredIfInProgress(taskId, now) == 0) {
            LOGGER.debug("Task {} was already updated by another run", taskId);
            return;
        }
        LOGGER.debug("Task {} expired", taskId);
        requestCommandService.handle(new CreateRequestCommand("La tarea venció automáticamente.", "EXPIRED", taskId));
    }
}
