package com.hub.synhub_backend.tasks;

import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.hub.synhub_backend.tasks.aplication.internal.commandservices.TaskCommandServiceImpl;
import com.hub.synhub_backend.tasks.domain.exceptions.TaskStatusTransitionNotAllowedException;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.commands.UpdateTaskStatusCommand;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** DONE solo se alcanza aprobando la entrega: el endpoint de status lo rechaza para todos. */
class TaskStatusDoneBlockedTest {

    @Test
    void directDoneIs409EvenForLeader() {
        var taskRepository = mock(TaskRepository.class);
        var task = mock(Task.class, RETURNS_DEEP_STUBS);
        when(task.getProfile().getGroup().getLeader().getId()).thenReturn(1L);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        var service = new TaskCommandServiceImpl(taskRepository, mock(UserRepository.class), mock(GroupRepository.class), mock(GroupProfileRepository.class));

        var e = assertThrows(TaskStatusTransitionNotAllowedException.class,
                () -> service.handle(new UpdateTaskStatusCommand(1L, 10L, TaskStatus.DONE)));
        assertEquals("A task can only be marked as DONE by approving its submission request", e.getMessage());
        verify(taskRepository, never()).save(any());
    }
}
