package com.hub.synhub_backend.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/** AUDIT ARC-04: el esquema lo crea Flyway y Hibernate solo lo valida; sin OSIV. */
class SchemaMigrationApiTests extends ApiTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    @Value("${spring.jpa.hibernate.ddl-auto}")
    private String ddlAuto;

    @Value("${spring.jpa.open-in-view}")
    private boolean openInView;

    @Test void schemaComesFromFlywayMigrations() {
        assertEquals("validate", ddlAuto, "Hibernate no crea ni cambia tablas");
        assertFalse(openInView);
        var applied = jdbc.queryForList("select version from flyway_schema_history where success order by installed_rank", String.class);
        assertTrue(applied.contains("1"), "V1__init aplicada: " + applied);
        var indexes = jdbc.queryForList("select indexname from pg_indexes where tablename = 'tasks'", String.class);
        assertTrue(indexes.contains("ix_tasks_status_due_date"), "índice del scheduler: " + indexes);
    }
}
