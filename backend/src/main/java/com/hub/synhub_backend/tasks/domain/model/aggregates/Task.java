package com.hub.synhub_backend.tasks.domain.model.aggregates;

import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.requests.domain.model.aggregates.Request;
import com.hub.synhub_backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.hub.synhub_backend.tasks.domain.exceptions.TaskFinishedException;
import com.hub.synhub_backend.tasks.domain.model.commands.CreateTaskCommand;
import com.hub.synhub_backend.tasks.domain.model.commands.UpdateTaskCommand;
import com.hub.synhub_backend.tasks.domain.model.commands.UpdateTaskStatusCommand;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

/**
 * Borrado lógico (decisión HUM-03): {@code delete} marca {@code deleted_at} y Hibernate excluye la fila
 * de todas las consultas, cargas y colecciones. Los datos siguen en la base.
 */
@Getter
@Entity
@SoftDelete(strategy = SoftDeleteType.TIMESTAMP, columnName = "deleted_at")
public class Task extends AuditableAbstractAggregateRoot<Task> {
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Setter
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(nullable = false)
    private OffsetDateTime dueDate;

    @Setter
    @ManyToOne
    @JoinColumn(name = "profile_id")
    private GroupProfile profile;

    // Clasificación sugerida por IA (nullable: tareas previas o sin sugerencia). labels = CSV.
    private Integer difficulty;
    private String context;
    private String urgency;
    private String labels;
    private Boolean aiAccepted;

    // Justificación que dio la IA al sugerir la clasificación; nula si se clasificó a mano.
    @Column(length = 500)
    private String aiRationale;

    @Column(nullable = false)
    private Integer timesRearranged = 0;

    @Column(nullable = false)
    private Long timePassed = 0L;

    public Task() {
        this.status = TaskStatus.IN_PROGRESS;
        this.timesRearranged = 0;
    }

    public Task(CreateTaskCommand command) {
        if (command.title() == null || command.title().isBlank())
            throw new IllegalArgumentException("A task needs a title");
        if (command.dueDate() == null) throw new IllegalArgumentException("A task needs a due date");
        this.title = command.title();
        this.description = command.description() == null ? "" : command.description();
        this.dueDate = command.dueDate();
        this.status = TaskStatus.IN_PROGRESS;
        this.updateClassification(
                command.difficulty(),
                command.context(),
                command.urgency(),
                command.labels(),
                command.aiAccepted(),
                command.aiRationale());
    }

    /** Resultado de revisar la entrega: aprobada -> DONE; rechazada -> IN_PROGRESS (suma un reagendado). */
    public void applySubmissionReview(boolean approved) {
        this.updateStatus(
                new UpdateTaskStatusCommand(null, this.getId(), approved ? TaskStatus.DONE : TaskStatus.IN_PROGRESS));
    }

    /**
     * Una tarea DONE (entrega aprobada) es definitiva: no se edita, no cambia de estado ni se borra (AUDIT BUG-18).
     * La web ya lo impedía; ahora lo exige el dominio.
     */
    public void ensureNotFinished() {
        if (this.status == TaskStatus.DONE) throw new TaskFinishedException(this.getId());
    }

    public void updateStatus(UpdateTaskStatusCommand command) {

        var commandStatus = command.status();

        if (this.status == TaskStatus.IN_PROGRESS && commandStatus == TaskStatus.COMPLETED) {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            if (timesRearranged > 0) {
                long updatedAt = this.getUpdatedAt().toInstant().toEpochMilli();
                this.timePassed += now.toInstant().toEpochMilli() - updatedAt;
            } else {
                this.timePassed = now.toInstant().toEpochMilli()
                        - this.getCreatedAt().toInstant().toEpochMilli();
            }
        } else if (this.status == TaskStatus.COMPLETED && commandStatus == TaskStatus.IN_PROGRESS) {
            timesRearranged++;
        } else if (this.status == TaskStatus.ON_HOLD && commandStatus == TaskStatus.IN_PROGRESS) {
            timesRearranged++;
        } else if (this.status == TaskStatus.EXPIRED && commandStatus == TaskStatus.IN_PROGRESS) {
            timesRearranged++;
        }
        this.status = command.status();
    }

    public void updateTask(UpdateTaskCommand command) {
        if (command.title() != null && !command.title().isEmpty()) {
            this.title = command.title();
        }
        if (command.description() != null && !command.description().isEmpty()) {
            this.description = command.description();
        }
        if (command.classification() != null) {
            var c = command.classification();
            this.updateClassification(
                    c.difficulty(), c.context(), c.urgency(), c.labels(), c.aiAccepted(), c.aiRationale());
        }
        if (command.dueDate() != null) {
            this.dueDate = command.dueDate();
            if (command.dueDate().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
                if (this.status != TaskStatus.EXPIRED) {
                    this.status = TaskStatus.EXPIRED;
                }
            } else {
                if (this.status != TaskStatus.EXPIRED) {
                    this.status = TaskStatus.IN_PROGRESS;
                }
            }
        }
    }

    /**
     * Reemplaza la clasificación; sin ningún campo, la tarea queda sin clasificar y sin medición de la IA.
     * La justificación solo se guarda si la clasificación viene de la IA (aiAccepted no nulo).
     */
    public void updateClassification(
            Integer difficulty, String context, String urgency, String labels, Boolean aiAccepted, String aiRationale) {
        this.difficulty = difficulty;
        this.context = blankToNull(context);
        this.urgency = blankToNull(urgency);
        this.labels = blankToNull(labels);
        boolean classified =
                this.difficulty != null || this.context != null || this.urgency != null || this.labels != null;
        this.aiAccepted = classified ? aiAccepted : null;
        this.aiRationale = this.aiAccepted == null ? null : blankToNull(aiRationale);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
