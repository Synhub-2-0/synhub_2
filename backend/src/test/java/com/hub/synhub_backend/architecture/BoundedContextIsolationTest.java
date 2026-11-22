package com.hub.synhub_backend.architecture;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * AUDIT ARC-01: un bounded context no usa la infraestructura (repositorios) de otro; lo que necesita llega por
 * las fachadas {@code interfaces/acl}. Las entidades JPA compartidas (por ejemplo, Task → GroupProfile) quedan
 * fuera de esta regla.
 */
class BoundedContextIsolationTest {

    private static final Path SOURCES = Path.of("src/main/java/com/hub/synhub_backend");
    private static final Pattern FOREIGN_INFRASTRUCTURE =
            Pattern.compile("import com\\.hub\\.synhub_backend\\.(\\w+)\\.infrastructure\\.persistence\\.");

    private static final Pattern SUBSCRIPTIONS_INTERNALS = Pattern.compile(
            "import com\\.hub\\.synhub_backend\\.subscriptions\\.(domain|application|infrastructure|interfaces\\.rest)\\.");

    @Test
    void theSubscriptionsContextExistsAndIsCoveredByTheRule() throws IOException {
        try (var files = Files.walk(SOURCES.resolve("subscriptions"))) {
            assertTrue(
                    files.anyMatch(f -> f.toString().endsWith("SubscriptionsContextFacadeImpl.java")),
                    "el contexto subscriptions existe y lo recorre la regla de aislamiento");
        }
    }

    @Test
    void otherContextsOnlyUseTheSubscriptionsFacade() throws IOException {
        var violations = new ArrayList<String>();
        try (var files = Files.walk(SOURCES)) {
            for (var file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (SOURCES.relativize(file).getName(0).toString().equals("subscriptions")) continue;
                if (SUBSCRIPTIONS_INTERNALS.matcher(Files.readString(file)).find())
                    violations.add(SOURCES.relativize(file).toString());
            }
        }
        assertTrue(violations.isEmpty(), "Solo interfaces/acl de subscriptions es público: " + violations);
    }

    @Test
    void noContextImportsAnotherContextsRepositories() throws IOException {
        var violations = new ArrayList<String>();
        try (var files = Files.walk(SOURCES)) {
            for (var file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                var context = SOURCES.relativize(file).getName(0).toString();
                var matcher = FOREIGN_INFRASTRUCTURE.matcher(Files.readString(file));
                while (matcher.find()) {
                    if (!matcher.group(1).equals(context))
                        violations.add(SOURCES.relativize(file) + " → " + matcher.group(1));
                }
            }
        }
        assertTrue(violations.isEmpty(), "Accesos a repositorios de otro contexto: " + violations);
    }
}
