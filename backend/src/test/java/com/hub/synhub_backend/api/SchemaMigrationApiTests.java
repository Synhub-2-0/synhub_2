package com.hub.synhub_backend.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

/** AUDIT ARC-04: el esquema lo crea Flyway y Hibernate solo lo valida; sin OSIV. */
class SchemaMigrationApiTests extends ApiTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String ddlAuto;

    @Value("${spring.jpa.open-in-view}")
    private boolean openInView;

    /** La parte de la V4 que bloquea los grupos que excedan 2 por líder, tal como está escrita en el archivo. */
    private static String backfillStatement() throws java.io.IOException {
        var sql = new String(
                new org.springframework.core.io.ClassPathResource("db/migration/V4__subscriptions_and_group_lock.sql")
                        .getInputStream()
                        .readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        var start = sql.indexOf("update groups");
        assertTrue(start >= 0, "la V4 contiene el backfill");
        return sql.substring(start, sql.indexOf(';', start));
    }

    @Test
    void backfillLocksTheGroupsAboveTheFreeLimitOfEachLeader() throws Exception {
        var busy = account("lider");
        var calm = account("tranquilo");
        var first = createGroup(busy);
        var second = createGroup(busy);
        var third = createGroup(busy);
        var fourth = createGroup(busy);
        var single = createGroup(calm);
        jdbc.update(
                "update groups set locked = false where id in (?, ?, ?, ?, ?)", first, second, third, fourth, single);

        jdbc.update(backfillStatement());

        java.util.function.LongFunction<Boolean> locked =
                id -> jdbc.queryForObject("select locked from groups where id = ?", Boolean.class, id);
        assertFalse(locked.apply(first), "los dos primeros siguen activos");
        assertFalse(locked.apply(second));
        assertTrue(locked.apply(third), "los que exceden 2 quedan bloqueados");
        assertTrue(locked.apply(fourth));
        assertFalse(locked.apply(single), "otro líder con un solo grupo no se toca");
    }

    @Test
    void schemaComesFromFlywayMigrations() {
        assertEquals("validate", ddlAuto, "Hibernate no crea ni cambia tablas");
        assertFalse(openInView);
        var applied = jdbc.queryForList(
                "select version from flyway_schema_history where success order by installed_rank", String.class);
        assertTrue(applied.contains("1"), "V1__init aplicada: " + applied);
        assertTrue(applied.contains("2"), "V2__task_ai_rationale aplicada: " + applied);
        assertTrue(applied.contains("3"), "V3__task_active_since aplicada: " + applied);
        var activeSince = jdbc.queryForList(
                "select column_name from information_schema.columns where table_name = 'tasks' and column_name = 'active_since'",
                String.class);
        assertEquals(1, activeSince.size(), "tasks.active_since existe");
        assertTrue(applied.contains("4"), "V4__subscriptions_and_group_lock aplicada: " + applied);
        var tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_name = 'subscriptions'", String.class);
        assertEquals(1, tables.size(), "existe la tabla subscriptions");
        var locked = jdbc.queryForList(
                "select column_name from information_schema.columns where table_name = 'groups' and column_name = 'locked'",
                String.class);
        assertEquals(1, locked.size(), "groups.locked existe");
        var indexes = jdbc.queryForList("select indexname from pg_indexes where tablename = 'tasks'", String.class);
        assertTrue(indexes.contains("ix_tasks_status_due_date"), "índice del scheduler: " + indexes);
    }
}
