package com.hub.synhub_backend.tasks;

import com.hub.synhub_backend.groups.domain.model.aggregates.Group;
import com.hub.synhub_backend.groups.domain.model.entities.GroupProfile;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupProfileRepository;
import com.hub.synhub_backend.groups.infrastructure.persistence.jpa.repositories.GroupRepository;
import com.hub.synhub_backend.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import com.hub.synhub_backend.requests.interfaces.acl.RequestsContextFacade;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import com.hub.synhub_backend.tasks.application.internal.commandservices.TaskCommandServiceImpl;
import com.hub.synhub_backend.tasks.domain.model.aggregates.Task;
import com.hub.synhub_backend.tasks.domain.model.commands.*;
import com.hub.synhub_backend.tasks.domain.model.valueobjects.TaskStatus;
import com.hub.synhub_backend.tasks.infrastructure.persistence.jpa.repositories.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.hub.synhub_backend.groups.GroupFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Casos de uso de escritura de tareas y sus permisos. */
class TaskCommandServiceImplTest {

    private static final long LEADER = 1, ANA = 2, LUIS = 3, STRANGER = 4, GROUP = 10, TASK = 100;
    private static final OffsetDateTime DUE = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);

    private TaskRepository tasks;
    private UserRepository users;
    private GroupRepository groups;
    private GroupProfileRepository profiles;
    private RequestsContextFacade requests;
    private TaskCommandServiceImpl service;
    private Group group;
    private GroupProfile anaProfile, luisProfile;
    private Task task;

    @BeforeEach
    void setUp() {
        tasks = mock(TaskRepository.class);
        users = mock(UserRepository.class);
        groups = mock(GroupRepository.class);
        profiles = mock(GroupProfileRepository.class);
        requests = mock(RequestsContextFacade.class);
        service = new TaskCommandServiceImpl(tasks, users, groups, profiles, requests);

        var ana = user(ANA, "ana");
        var luis = user(LUIS, "luis");
        group = group(GROUP, user(LEADER, "lider"), ana, luis);
        anaProfile = group.getUserProfiles().get(1);
        luisProfile = group.getUserProfiles().get(2);

        task = new Task(new CreateTaskCommand(LEADER, "Login", "desc", DUE, ANA, GROUP, null, null, null, null, null));
        ReflectionTestUtils.setField(task, "id", TASK);
        ReflectionTestUtils.setField(task, "createdAt", OffsetDateTime.now(ZoneOffset.UTC).minusHours(1));
        ReflectionTestUtils.setField(task, "updatedAt", OffsetDateTime.now(ZoneOffset.UTC).minusHours(1));
        anaProfile.addTask(task);

        when(users.findById(ANA)).thenReturn(Optional.of(ana));
        when(users.findById(LUIS)).thenReturn(Optional.of(luis));
        when(users.existsById(ANA)).thenReturn(true);
        when(groups.findById(GROUP)).thenReturn(Optional.of(group));
        when(groups.existsById(GROUP)).thenReturn(true);
        when(profiles.findByGroup_IdAndUser_Id(GROUP, ANA)).thenReturn(Optional.of(anaProfile));
        when(profiles.findByGroup_IdAndUser_Id(GROUP, LUIS)).thenReturn(Optional.of(luisProfile));
        when(tasks.findById(TASK)).thenReturn(Optional.of(task));
        when(tasks.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateTaskCommand createFor(long requester, long assignee) {
        return new CreateTaskCommand(requester, "Nueva", "desc", DUE, assignee, GROUP, 2, "QA", "LOW", "tests", false);
    }

    // ---- Crear
    @Test void leaderCreatesTaskForMember() {
        var created = service.handle(createFor(LEADER, LUIS));
        assertSame(luisProfile, created.getProfile());
        verify(tasks).save(created);
    }

    @Test void memberCannotCreateTasks() {
        assertThrows(AccessDeniedException.class, () -> service.handle(createFor(ANA, LUIS)));
        verify(tasks, never()).save(any());
    }

    @Test void assigneeMustBelongToTheGroup() {
        when(users.findById(STRANGER)).thenReturn(Optional.of(user(STRANGER, "ajeno")));
        when(profiles.findByGroup_IdAndUser_Id(GROUP, STRANGER)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.handle(createFor(LEADER, STRANGER)));
    }

    // ---- Editar
    @Test void leaderReassignsAndEdits() {
        var updated = service.handle(new UpdateTaskCommand(LEADER, TASK, "Login v2", "", null, LUIS));
        assertSame(luisProfile, updated.getProfile());
        assertEquals("Login v2", updated.getTitle());
    }

    @Test void memberCannotEditTaskDetails() {
        assertThrows(AccessDeniedException.class, () -> service.handle(new UpdateTaskCommand(ANA, TASK, "x", "", null, ANA)));
    }

    // ---- Borrar
    @Test void leaderDeletesTask() {
        service.handle(new DeleteTaskCommand(LEADER, TASK));
        // AUDIT BUG-01: primero las requests (FK), después la tarea
        var order = inOrder(requests, tasks);
        order.verify(requests).deleteAllRequestsByTaskId(TASK);
        order.verify(tasks).delete(task);
        // HUM-03: soft delete; la tarea conserva su responsable
        assertSame(anaProfile, task.getProfile());
    }

    @Test void memberCannotDeleteTask() {
        assertThrows(AccessDeniedException.class, () -> service.handle(new DeleteTaskCommand(ANA, TASK)));
        verify(tasks, never()).delete(any(Task.class));
    }

    // ---- Estado
    @Test void assignedMemberAndLeaderCanChangeStatus() {
        assertEquals(TaskStatus.COMPLETED, service.handle(new UpdateTaskStatusCommand(ANA, TASK, TaskStatus.COMPLETED)).getStatus());
        assertEquals(TaskStatus.IN_PROGRESS, service.handle(new UpdateTaskStatusCommand(LEADER, TASK, TaskStatus.IN_PROGRESS)).getStatus());
    }

    @Test void otherMembersCannotChangeStatus() {
        assertThrows(AccessDeniedException.class, () -> service.handle(new UpdateTaskStatusCommand(LUIS, TASK, TaskStatus.COMPLETED)));
        assertThrows(AccessDeniedException.class, () -> service.handle(new UpdateTaskStatusCommand(STRANGER, TASK, TaskStatus.COMPLETED)));
    }

    @Test void unknownTaskIs404() {
        when(tasks.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.handle(new UpdateTaskStatusCommand(LEADER, 999L, TaskStatus.ON_HOLD)));
    }

    // ---- Borrar tareas de un integrante
    @Test void deletesAllTasksOfAMemberInAGroup() {
        when(tasks.findByProfile_Id(anaProfile.getId())).thenReturn(List.of(task));
        service.handle(new DeleteTasksByUserIdAndGroupIdCommand(ANA, GROUP));
        var order = inOrder(requests, tasks);
        order.verify(requests).deleteAllRequestsByTaskId(TASK);
        order.verify(tasks).delete(task);
    }

    @Test void deletingTasksOfUnknownUserFails() {
        assertThrows(IllegalArgumentException.class, () -> service.handle(new DeleteTasksByUserIdAndGroupIdCommand(99L, GROUP)));
    }

    // ---- AUDIT BUG-02: borrar todas las tareas de un grupo
    @Test void deletesAllTasksOfAGroupWithTheirRequests() {
        when(tasks.findByProfile_Group_Id(GROUP)).thenReturn(List.of(task));
        service.handle(new DeleteTasksByGroupIdCommand(GROUP));
        var order = inOrder(requests, tasks);
        order.verify(requests).deleteAllRequestsByTaskId(TASK);
        order.verify(tasks).delete(task);
        assertFalse(((List<?>) ReflectionTestUtils.getField(anaProfile, "tasks")).contains(task));
    }
}
