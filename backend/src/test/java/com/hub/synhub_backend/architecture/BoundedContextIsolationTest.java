package com.hub.synhub_backend.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AUDIT ARC-01: un bounded context no usa la infraestructura (repositorios) de otro; lo que necesita llega por
 * las fachadas {@code interfaces/acl}. Las entidades JPA compartidas (por ejemplo, Task → GroupProfile) quedan
 * fuera de esta regla.
 */
class BoundedContextIsolationTest {

    private static final Path SOURCES = Path.of("src/main/java/com/hub/synhub_backend");
    private static final Pattern FOREIGN_INFRASTRUCTURE =
            Pattern.compile("import com\\.hub\\.synhub_backend\\.(\\w+)\\.infrastructure\\.persistence\\.");

    @Test void noContextImportsAnotherContextsRepositories() throws IOException {
        var violations = new ArrayList<String>();
        try (var files = Files.walk(SOURCES)) {
            for (var file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                var context = SOURCES.relativize(file).getName(0).toString();
                var matcher = FOREIGN_INFRASTRUCTURE.matcher(Files.readString(file));
                while (matcher.find()) {
                    if (!matcher.group(1).equals(context)) violations.add(SOURCES.relativize(file) + " → " + matcher.group(1));
                }
            }
        }
        assertTrue(violations.isEmpty(), "Accesos a repositorios de otro contexto: " + violations);
    }
}
