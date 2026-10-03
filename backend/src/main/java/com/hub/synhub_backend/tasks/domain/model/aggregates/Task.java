package com.hub.synhub_backend.tasks.domain.model.aggregates;

import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.hub.synhub_backend.tasks.domain.model.commands.CreateTaskCommand;
import com.hub.synhub_backend.tasks.domain.model.commands.UpdateTaskCommand;
import com.hub.synhub_backend.tasks.domain.model.commands.UpdateTaskStatusCommand;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Getter
@Entity
public class Task extends AuditableAbstractAggregateRoot<Task> {
    @NonNull
    private String title;

    @NonNull
    private String description;

    @Setter
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @NonNull
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

    @Column(nullable = false)
    private Integer timesRearranged = 0;

    @Column(nullable = false)
    private Long timePassed = 0L;

    public Task() {
        this.status = TaskStatus.IN_PROGRESS;
        this.timesRearranged = 0;
    }

    public Task(CreateTaskCommand command) {
        this.title = command.title();
        this.description = command.description();
        this.dueDate = command.dueDate();
        this.status = TaskStatus.IN_PROGRESS;
        this.difficulty = command.difficulty();
        this.context = command.context();
        this.urgency = command.urgency();
        this.labels = command.labels();
        this.aiAccepted = command.aiAccepted();
    }

    public void updateStatus(UpdateTaskStatusCommand command) {

        var commandStatus = command.status();

        if(this.status == TaskStatus.IN_PROGRESS && commandStatus == TaskStatus.COMPLETED) {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            if (timesRearranged > 0) {
                long updatedAt = this.getUpdatedAt().toInstant().toEpochMilli();
                this.timePassed += now.toInstant().toEpochMilli() - updatedAt;
            } else {
                this.timePassed = now.toInstant().toEpochMilli() - this.getCreatedAt().toInstant().toEpochMilli();
            }
        } else if(this.status == TaskStatus.COMPLETED && commandStatus == TaskStatus.IN_PROGRESS) {
            timesRearranged++;
        } else if (this.status == TaskStatus.ON_HOLD && commandStatus == TaskStatus.IN_PROGRESS) {
            timesRearranged++;
        } else if (this.status == TaskStatus.EXPIRED && commandStatus == TaskStatus.IN_PROGRESS) {
            timesRearranged++;
        }
        this.status = command.status();
    }

    public void updateTask(UpdateTaskCommand command) {
        if(command.title() != null && !command.title().isEmpty()) {
            this.title = command.title();
        }
        if(command.description() != null && !command.description().isEmpty()) {
            this.description = command.description();
        }
        if(command.dueDate() != null) {
            this.dueDate = command.dueDate();
            if(command.dueDate().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
                if(this.status != TaskStatus.EXPIRED) {
                    this.status = TaskStatus.EXPIRED;
                }
            } else {
                if(this.status != TaskStatus.EXPIRED) {
                    this.status = TaskStatus.IN_PROGRESS;
                }
            }
        }
    }
}
