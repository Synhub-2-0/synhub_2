package com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories;

import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByMember_Id(Long memberId);
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByGroup_Id(Long groupId);
    List<Task> findAllByStatusAndDueDateBefore(TaskStatus status, OffsetDateTime dueDate);
}
