package com.hub.synhub_backend.iam.interfaces.rest;

import com.hub.synhub_backend.iam.domain.services.UserCommandService;
import com.hub.synhub_backend.iam.infrastructure.tokens.jwt.SessionCookieService;
import com.hub.synhub_backend.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.hub.synhub_backend.iam.interfaces.rest.resources.SignInResource;
import com.hub.synhub_backend.iam.interfaces.rest.resources.SignUpResource;
import com.hub.synhub_backend.iam.interfaces.rest.resources.UserResource;
import com.hub.synhub_backend.iam.interfaces.rest.transform.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AuthenticationController
 * <p>
 *     Registro e inicio y cierre de sesión. La sesión es una cookie HttpOnly de 1 día (HUM-04):
 *     <ul>
 *         <li>POST /api/v1/authentication/sign-in: abre la sesión (Set-Cookie)</li>
 *         <li>POST /api/v1/authentication/sign-up</li>
 *         <li>POST /api/v1/authentication/sign-out: borra la cookie</li>
 *     </ul>
 * </p>
 */
@RestController
@RequestMapping(value = "/api/v1/authentication", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Authentication Endpoints")
public class AuthenticationController {

    private final UserCommandService userCommandService;
    private final SessionCookieService sessionCookieService;

    public AuthenticationController(UserCommandService userCommandService, SessionCookieService sessionCookieService) {
        this.userCommandService = userCommandService;
        this.sessionCookieService = sessionCookieService;
    }

    /**
     * Handles the sign-in request.
     * @param signInResource the sign-in request body.
     * @return the authenticated user resource.
     */
    @PostMapping("/sign-in")
    public ResponseEntity<AuthenticatedUserResource> signIn(@Valid @RequestBody SignInResource signInResource) {

        var signInCommand = SignInCommandFromResourceAssembler.toCommandFromResource(signInResource);
        var authenticatedUser = userCommandService.handle(signInCommand);

        var cookie = sessionCookieService.create(authenticatedUser.get().getRight());
        var authenticatedUserResource = AuthenticatedUserResourceFromEntityAssembler.toResourceFromEntity(
                authenticatedUser.get().getLeft(), sessionCookieService.expiresAtFromNow());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(authenticatedUserResource);
    }

    /**
     * Cierra la sesión borrando la cookie. Es pública: funciona aunque la sesión ya haya vencido.
     */
    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionCookieService.clear().toString())
                .build();
    }

    /**
     * Handles the sign-up request.
     * @param signUpResource the sign-up request body.
     * @return the created user resource.
     */
    @PostMapping("/sign-up")
    public ResponseEntity<UserResource> signUp(@Valid @RequestBody SignUpResource signUpResource) {
        var signUpCommand = SignUpCommandFromResourceAssembler.toCommandFromResource(signUpResource);
        var user = userCommandService.handle(signUpCommand);
        if (user.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user.get());
        return new ResponseEntity<>(userResource, HttpStatus.CREATED);
    }
}
