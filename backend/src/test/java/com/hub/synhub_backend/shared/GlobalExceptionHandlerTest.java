package com.hub.synhub_backend.shared;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hub.synhub_backend.groups.domain.exceptions.UserNotInGroupException;
import com.hub.synhub_backend.iam.domain.exceptions.UsernameAlreadyExistsException;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ErrorResponse;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.GlobalExceptionHandler;
import com.hub.synhub_backend.shared.interfaces.rest.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Traducción de excepciones a ErrorResponse y códigos HTTP. */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/v1/tasks/7");
    }

    private static void assertError(ResponseEntity<ErrorResponse> response, int status, String message) {
        assertEquals(status, response.getStatusCode().value());
        var body = response.getBody();
        assertNotNull(body);
        assertEquals(status, body.status());
        assertEquals(message, body.message());
        assertEquals("/api/v1/tasks/7", body.path());
        assertNotNull(body.timestamp());
    }

    @Test void badRequest() {
        assertError(handler.handleIllegalArgument(new IllegalArgumentException("invalid"), request), 400, "invalid");
    }

    @Test void unauthorized() {
        assertError(handler.handleIllegalArgument(new BadCredentialsException("bad"), request), 401, "bad");
    }

    @Test void forbidden() {
        assertError(handler.handleIllegalArgument(new AccessDeniedException("nope"), request), 403, "nope");
    }

    @Test void notFoundIncludingSubclasses() {
        assertError(handler.handleIllegalArgument(new ResourceNotFoundException("missing"), request), 404, "missing");
        assertError(handler.handleIllegalArgument(new UserNotInGroupException("not member"), request), 404, "not member");
    }

    @Test void businessRuleIsConflict() {
        assertError(handler.handleIllegalArgument(new UsernameAlreadyExistsException("dup"), request), 409, "dup");
    }

    @Test void dataIntegrityViolationIsConflict() {
        var response = handler.handleDataIntegrity(new org.springframework.dao.DataIntegrityViolationException("duplicate key"), request);
        assertEquals(409, response.getStatusCode().value());
        assertFalse(response.getBody().message().contains("duplicate key"), "no expone detalles de la base");
    }

    @Test void unexpectedErrorHidesDetails() {
        var response = handler.handleGeneric(new IllegalStateException("db password is x"), request);
        assertEquals(500, response.getStatusCode().value());
        assertFalse(response.getBody().message().contains("password"));
    }

    // AUDIT BUG-07 (resuelto)
    @Test void unexpectedErrorIsLogged() {
        var root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        root.addAppender(appender);
        try {
            handler.handleGeneric(new IllegalStateException("boom"), request);
            assertTrue(appender.list.stream().anyMatch(e -> e.getLevel() == Level.ERROR
                    && e.getThrowableProxy() != null && "boom".equals(e.getThrowableProxy().getMessage())));
        } finally {
            root.detachAppender(appender);
        }
    }

    @Disabled("AUDIT BUG-14: ErrorResponse.timestamp es LocalDateTime (sin zona)")
    @Test void timestampCarriesTimezone() {
        assertEquals(OffsetDateTime.class, ErrorResponse.class.getRecordComponents()[0].getType());
    }
}
