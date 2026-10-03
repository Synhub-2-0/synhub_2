package com.hub.synhub_backend.ai;

import com.hub.synhub_backend.ai.application.internal.queryservices.TaskClassificationQueryServiceImpl;
import com.hub.synhub_backend.ai.domain.model.queries.ClassifyTaskQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import static org.junit.jupiter.api.Assertions.*;

// Llama a Bedrock de verdad: correr con RUN_BEDROCK_IT=1 y credenciales AWS.
@EnabledIfEnvironmentVariable(named = "RUN_BEDROCK_IT", matches = "1")
class TaskClassificationQueryServiceImplIT {

    @Test
    void classifiesTask() {
        var client = BedrockRuntimeClient.builder().region(Region.US_EAST_1).build();
        var service = new TaskClassificationQueryServiceImpl(client, "us.anthropic.claude-haiku-4-5-20251001-v1:0");

        var result = service.handle(new ClassifyTaskQuery(
                "Migrar la tabla de usuarios a UUID",
                "Cambiar PK de bigint a uuid en PostgreSQL con migración sin downtime, urgente para el release", null));

        assertTrue(result.isPresent());
        System.out.println(result.get());
        assertTrue(result.get().difficulty() >= 1 && result.get().difficulty() <= 5);
        assertEquals("DATABASE", result.get().context());
    }
}
