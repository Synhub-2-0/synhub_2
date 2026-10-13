package com.hub.synhub_backend.api;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AUDIT PERF-01: los listados ejecutan un número de consultas SQL que no crece con la cantidad de elementos.
 * Cada escenario se mide con pocos y con muchos elementos (de distintos integrantes, para que la caché de
 * primer nivel no oculte un N+1) y se exige la misma cantidad de sentencias.
 */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class QueryCountApiTests extends ApiTestSupport {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void statistics() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private long statementsOf(Supplier<Res> call) {
        statistics.clear();
        var res = call.get();
        assertEquals(200, res.status(), res.raw());
        return statistics.getPrepareStatementCount();
    }

    /** Un grupo con {@code members} integrantes, cada uno con una tarea entregada. */
    private record Scenario(Account leader, long groupId, List<Long> taskIds) {}

    private Scenario group(int members) {
        var leader = account("lider");
        var groupId = createGroup(leader);
        var tasks = new ArrayList<Long>();
        for (int i = 0; i < members; i++) {
            var member = account("m" + i);
            join(leader, member, groupId);
            var taskId = createTask(leader, groupId, member);
            submit(member, taskId);
            tasks.add(taskId);
        }
        return new Scenario(leader, groupId, tasks);
    }

    @Test void groupTasksUseAConstantNumberOfQueries() {
        var small = group(1);
        var large = group(5);
        var few = statementsOf(() -> get("/tasks/group/" + small.groupId(), small.leader().token()));
        var many = statementsOf(() -> get("/tasks/group/" + large.groupId(), large.leader().token()));
        assertEquals(few, many, "GET /tasks/group/{id}: " + few + " sentencias con 1 tarea y " + many + " con 5");
    }

    @Test void groupRequestsUseAConstantNumberOfQueries() {
        var small = group(1);
        var large = group(5);
        var few = statementsOf(() -> get("/groups/" + small.groupId() + "/requests?status=PENDING&type=SUBMISSION", small.leader().token()));
        var many = statementsOf(() -> get("/groups/" + large.groupId() + "/requests?status=PENDING&type=SUBMISSION", large.leader().token()));
        assertEquals(few, many, "GET /groups/{id}/requests: " + few + " sentencias con 1 request y " + many + " con 5");
    }

    @Test void groupsOfALeaderUseAConstantNumberOfQueries() {
        var leader = account("lider");
        var other = account("lider2");
        join(leader, account("y"), createGroup(leader));
        for (int i = 0; i < 4; i++) {
            var groupId = createGroup(other);
            join(other, account("x" + i), groupId);
        }
        var few = statementsOf(() -> get("/groups/user/role?groupRole=GROUP_LEADER", leader.token()));
        var many = statementsOf(() -> get("/groups/user/role?groupRole=GROUP_LEADER", other.token()));
        assertEquals(few, many, "GET /groups/user/role: " + few + " sentencias con 1 grupo y " + many + " con 4");
    }

    @Test void membersOfAGroupUseAConstantNumberOfQueries() {
        var small = group(1);
        var large = group(5);
        var few = statementsOf(() -> get("/groups/" + small.groupId() + "/members", small.leader().token()));
        var many = statementsOf(() -> get("/groups/" + large.groupId() + "/members", large.leader().token()));
        assertEquals(few, many, "GET /groups/{id}/members: " + few + " sentencias con 2 integrantes y " + many + " con 6");
    }

    @Test void invitationsOfAGroupUseAConstantNumberOfQueries() {
        var small = group(0);
        var large = group(0);
        post("/invitations", small.leader().token(), Map.of("userId", account("i").id(), "groupId", small.groupId()));
        for (int i = 0; i < 4; i++) {
            post("/invitations", large.leader().token(), Map.of("userId", account("i" + i).id(), "groupId", large.groupId()));
        }
        var few = statementsOf(() -> get("/invitations/group?groupId=" + small.groupId(), small.leader().token()));
        var many = statementsOf(() -> get("/invitations/group?groupId=" + large.groupId(), large.leader().token()));
        assertEquals(few, many, "GET /invitations/group: " + few + " sentencias con 1 invitación y " + many + " con 4");
    }
}
