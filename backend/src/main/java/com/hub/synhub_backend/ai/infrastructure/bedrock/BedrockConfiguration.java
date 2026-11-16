package com.hub.synhub_backend.ai.infrastructure.bedrock;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
public class BedrockConfiguration {

    // Credenciales por la cadena por defecto de AWS (env vars, perfil o rol IAM).
    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient(@Value("${ai.bedrock.region}") String region) {
        // Sin límite, una llamada colgada deja la petición del usuario esperando indefinidamente.
        return BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .overrideConfiguration(
                        c -> c.apiCallAttemptTimeout(Duration.ofSeconds(20)).apiCallTimeout(Duration.ofSeconds(30)))
                .build();
    }
}
