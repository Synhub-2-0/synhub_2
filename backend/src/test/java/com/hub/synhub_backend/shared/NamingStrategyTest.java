package com.hub.synhub_backend.shared;

import static org.junit.jupiter.api.Assertions.*;

import com.hub.synhub_backend.shared.infrastructure.persistence.jpa.configuration.strategy.SnakeCaseWithPluralizedTablePhysicalNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.junit.jupiter.api.Test;

/** Nombres físicos de tablas y columnas. */
class NamingStrategyTest {

    private final SnakeCaseWithPluralizedTablePhysicalNamingStrategy strategy =
            new SnakeCaseWithPluralizedTablePhysicalNamingStrategy();

    @Test
    void tablesArePluralizedSnakeCase() {
        assertEquals(
                "group_profiles",
                strategy.toPhysicalTableName(Identifier.toIdentifier("GroupProfile"), null)
                        .getText());
        assertEquals(
                "tasks",
                strategy.toPhysicalTableName(Identifier.toIdentifier("Task"), null)
                        .getText());
    }

    @Test
    void columnsAreSnakeCase() {
        assertEquals(
                "due_date",
                strategy.toPhysicalColumnName(Identifier.toIdentifier("dueDate"), null)
                        .getText());
        assertEquals(
                "times_rearranged",
                strategy.toPhysicalColumnName(Identifier.toIdentifier("timesRearranged"), null)
                        .getText());
    }

    @Test
    void nullIdentifiersStayNull() {
        assertNull(strategy.toPhysicalColumnName(null, null));
        assertNull(strategy.toPhysicalSchemaName(null, null));
    }
}
