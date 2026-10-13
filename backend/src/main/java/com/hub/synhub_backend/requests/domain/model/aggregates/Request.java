package com.hub.synhub_backend.requests.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import com.hub.synhub_backend.requests.domain.model.commands.CreateRequestCommand;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestStatus;
import com.hub.synhub_backend.requests.domain.model.valueobjects.RequestType;
import com.hub.synhub_backend.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;

@Getter
@Entity
@Table(name = "requests")
public class Request extends AuditableAbstractAggregateRoot<Request> {
    @NonNull
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type")
    private RequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status")
    private RequestStatus requestStatus;

    @Setter
    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    public Request() {
    }

    public String getRequestType() {
        return requestType.toString();
    }

    public String getRequestStatus() {
        return requestStatus.toString();
    }

    public Request(CreateRequestCommand command) {
        this.description = command.description();
        this.requestType = RequestType.fromString(command.requestType());
        this.requestStatus = RequestStatus.PENDING;
    }

    public void updateRequestStatus(String requestStatus) {
        this.requestStatus = RequestStatus.fromString(requestStatus);
    }
}
