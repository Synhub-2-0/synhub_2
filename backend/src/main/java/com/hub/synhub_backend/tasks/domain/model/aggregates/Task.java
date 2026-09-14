package com.hub.synhub_backend.tasks.domain.model.aggregates;

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
    @JoinColumn(name = "member_id")
    private Member member;

    @Setter
    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

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
    }

    public void updateStatus(UpdateTaskStatusCommand command) {
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
        //System.out.printf("[updateStatus] Antes: status=%s, timesRearranged=%d\n", this.status, this.timesRearranged);
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");

        var commandStatus = TaskStatus.valueOf(command.status());

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
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
            //System.out.printf("Task rearranged from COMPLETED to IN_PROGRESS. Times rearranged: %d. Task title: %s\n", timesRearranged, "Task tile: " + this.title);
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
        } else if (this.status == TaskStatus.ON_HOLD && commandStatus == TaskStatus.IN_PROGRESS) {
            timesRearranged++;
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
            //System.out.printf("Task rearranged from ON_HOLD to IN_PROGRESS. Times rearranged: %d. Task title: %s\n", timesRearranged, "Task tile: " + this.title);
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
        } else if (this.status == TaskStatus.EXPIRED && commandStatus == TaskStatus.IN_PROGRESS) {
            timesRearranged++;
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
            //System.out.printf("Task rearranged from EXPIRED to IN_PROGRESS. Times rearranged: %d. Task title: %s\n", timesRearranged, "Task tile: " + this.title);
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
            //System.out.printf("===============================================================================\n");
        }
        this.status = TaskStatus.valueOf(command.status());
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
        //System.out.printf("[updateTask] Después: status=%s, timesRearranged=%d, dueDate=%s\n", this.status, this.timesRearranged, this.dueDate);
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
    }

    public void updateTask(UpdateTaskCommand command) {
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
        //System.out.printf("[updateTask] Antes: status=%s, timesRearranged=%d\n", this.status, this.timesRearranged);
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
        if(command.title() != null && command.title() != "") {
            this.title = command.title();
        }
        if(command.description() != null && command.description() != "") {
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
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
        //System.out.printf("[updateTask] Después: status=%s, timesRearranged=%d, dueDate=%s\n", this.status, this.timesRearranged, this.dueDate);
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
        //System.out.printf("===============================================================================\n");
    }
}
