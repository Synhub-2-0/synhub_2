package com.hub.synhub_backend.tasks.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.hub.synhub_backend.iam.infrastructure.tokens.jwt.BearerTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/tests")
@Tag(name = "Tests", description = "Test endpoints")
public class TestController {

    private final BearerTokenService tokenService;

    public TestController(BearerTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping("/fetch-user")
    public String ejemplo(Authentication authentication) {
        String username = authentication.getName();
        return "Usuario: " + username;
    }

    @PostMapping("/token")
    @Operation(summary = "Genera un nuevo token JWT", description = "Genera un nuevo token para el usuario autenticado sin invalidar el anterior")
    public ResponseEntity<String> generateNewToken(Authentication authentication) {
        String username = authentication.getName();
        // Suponiendo que tienes acceso a BearerTokenService
        String newToken = tokenService.generateToken(username);
        return ResponseEntity.ok(newToken);
    }
}
