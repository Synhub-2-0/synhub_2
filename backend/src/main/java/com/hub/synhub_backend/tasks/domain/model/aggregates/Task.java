package com.hub.synhub_backend.tasks.domain.model.aggregates;

import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.hub.synhub_backend.tasks.domain.exceptions.TaskFinishedException;
import com.hub.synhub_backend.tasks.domain.model.commands.CreateTaskCommand;
import com.hub.synhub_backend.tasks.domain.model.commands.UpdateTaskCommand;
import com.hub.synhub_backend.tasks.domain.model.commands.UpdateTaskStatusCommand;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import jakarta.persistence.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
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

    /** Milisegundos acumulados en los que la tarea estuvo IN_PROGRESS (no cuenta pausas, ni vencida, ni completada). */
    @Column(nullable = false)
    private Long timePassed = 0L;

    /** Desde cuándo corre el tiempo activo: solo tiene valor mientras la tarea está IN_PROGRESS. */
    private OffsetDateTime activeSince;

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
        this.activeSince = OffsetDateTime.now(ZoneOffset.UTC);
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

        if (commandStatus == TaskStatus.IN_PROGRESS && this.status != TaskStatus.IN_PROGRESS) {
            // Volver a IN_PROGRESS (desde COMPLETED, ON_HOLD o EXPIRED) cuenta como reagendado.
            timesRearranged++;
        }
        moveTo(commandStatus);
    }

    /**
     * Cambia el estado y mantiene el reloj del tiempo activo: corre solo en IN_PROGRESS. Al salir de IN_PROGRESS
     * se suma lo transcurrido (hasta la fecha límite si ya venció) y al volver a entrar empieza de nuevo.
     */
    private void moveTo(TaskStatus target) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        if (this.status == TaskStatus.IN_PROGRESS && target != TaskStatus.IN_PROGRESS) {
            closeActiveTime(now);
        } else if (this.status != TaskStatus.IN_PROGRESS && target == TaskStatus.IN_PROGRESS) {
            this.activeSince = now;
        }
        this.status = target;
    }

    /** Suma el tiempo activo pendiente, sin pasar de la fecha límite, y detiene el reloj (no hace nada si ya está detenido). */
    private void closeActiveTime(OffsetDateTime now) {
        if (this.activeSince == null) return;
        var end = now.isBefore(this.dueDate) ? now : this.dueDate;
        this.timePassed += Math.max(0L, Duration.between(this.activeSince, end).toMillis());
        this.activeSince = null;
    }

    /**
     * Cierra el tiempo activo de una tarea que el vencimiento automático ya marcó EXPIRED: cuenta hasta su fecha
     * límite. Es idempotente.
     */
    public void settleActiveTime() {
        closeActiveTime(OffsetDateTime.now(ZoneOffset.UTC));
    }

    /** Lo que el líder edita de una tarea: sirve para saber si una edición cambió algo. */
    private List<Object> editableContent() {
        return Arrays.asList(
                title,
                description,
                dueDate == null ? null : dueDate.toInstant(),
                difficulty,
                context,
                urgency,
                labels,
                aiAccepted,
                aiRationale);
    }

    /**
     * Pasa a IN_PROGRESS una tarea en espera porque el líder la editó (resuelve su solicitud de modificación).
     * Si la edición ya la movió (nueva fecha límite), no hace nada.
     */
    public void resumeAfterEditing() {
        if (this.status == TaskStatus.ON_HOLD) {
            this.updateStatus(new UpdateTaskStatusCommand(null, this.getId(), TaskStatus.IN_PROGRESS));
        }
    }

    /** Aplica la edición del líder. Devuelve si cambió algún dato (título, descripción, fecha o clasificación). */
    public boolean updateTask(UpdateTaskCommand command) {
        var before = editableContent();
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
        // Una fecha igual a la actual no mueve el estado: la web siempre envía la fecha al editar.
        if (command.dueDate() != null
                && (this.dueDate == null
                        || !this.dueDate.toInstant().equals(command.dueDate().toInstant()))) {
            this.dueDate = command.dueDate();
            if (this.status != TaskStatus.EXPIRED) {
                moveTo(
                        command.dueDate().isBefore(OffsetDateTime.now(ZoneOffset.UTC))
                                ? TaskStatus.EXPIRED
                                : TaskStatus.IN_PROGRESS);
            }
        }
        return !before.equals(editableContent());
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
