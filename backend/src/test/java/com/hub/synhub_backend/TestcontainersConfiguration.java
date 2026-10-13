package com.hub.synhub_backend;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL efímero para las pruebas que levantan el contexto de Spring.
 * {@code @ServiceConnection} reemplaza el datasource de application.properties, así que basta con Docker.
 * Si se define DATABASE_URL (por ejemplo, para usar una base local sin Docker), se usa esa base y no se crea el contenedor.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    @ConditionalOnExpression("'${DATABASE_URL:}'.isEmpty()")
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:16-alpine");
    }
}
