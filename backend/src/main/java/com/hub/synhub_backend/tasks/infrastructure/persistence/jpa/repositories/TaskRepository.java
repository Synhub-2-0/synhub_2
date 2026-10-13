package com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories;

import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProfile_Id(Long profileId);
    List<Task> findByStatus(TaskStatus status);
    List<Task> findAllByStatusAndDueDateBefore(TaskStatus status, OffsetDateTime dueDate);
    List<Task> findByProfile_User_Id(Long profileUserId);
    List<Task> findByProfile_Group_Id(Long profileGroupId);
    List<Task> findByProfile_Group_IdAndProfile_User_Id(Long profileGroupId, Long profileUserId);
    List<Task> findByProfile_Group_IdAndStatus(Long profileGroupId, TaskStatus status);

    /**
     * Marca la tarea como EXPIRED solo si sigue IN_PROGRESS. Devuelve las filas afectadas (0 o 1): con varias
     * instancias del scheduler, solo una obtiene 1 (AUDIT BUG-09).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Task t set t.status = com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus.EXPIRED, t.updatedAt = :now "
            + "where t.id = :taskId and t.status = com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus.IN_PROGRESS")
    int markExpiredIfInProgress(@Param("taskId") Long taskId, @Param("now") OffsetDateTime now);
}
