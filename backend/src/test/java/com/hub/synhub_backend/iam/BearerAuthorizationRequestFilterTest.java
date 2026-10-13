package com.hub.synhub_backend.iam;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hub.synhub_backend.iam.infrastructure.authorization.sfs.pipeline.BearerAuthorizationRequestFilter;
import com.hub.synhub_backend.iam.infrastructure.tokens.jwt.services.TokenServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Filtro JWT: autentica la petición y nunca escribe el token en los logs (AUDIT SEC-03). */
class BearerAuthorizationRequestFilterTest {

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final List<Logger> loggers = List.of(
            (Logger) LoggerFactory.getLogger(BearerAuthorizationRequestFilter.class),
            (Logger) LoggerFactory.getLogger(TokenServiceImpl.class));
    private TokenServiceImpl tokens;
    private BearerAuthorizationRequestFilter filter;

    @BeforeEach
    void setUp() {
        appender.start();
        loggers.forEach(logger -> { logger.setLevel(Level.TRACE); logger.addAppender(appender); });
        tokens = new TokenServiceImpl();
        ReflectionTestUtils.setField(tokens, "secret", "test-secret-for-synhub-jwt-signing-0123456789abcdef");
        ReflectionTestUtils.setField(tokens, "expirationDays", 1);
        var users = mock(UserDetailsService.class);
        when(users.loadUserByUsername("ana")).thenReturn(User.withUsername("ana").password("x").roles("USER").build());
        filter = new BearerAuthorizationRequestFilter(tokens, users);
    }

    @AfterEach
    void tearDown() {
        loggers.forEach(logger -> { logger.detachAppender(appender); logger.setLevel(null); });
        SecurityContextHolder.clearContext();
    }

    private void run(String authorization) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        if (authorization != null) request.addHeader("Authorization", authorization);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    @Test void validTokenAuthenticatesTheRequest() throws Exception {
        run("Bearer " + tokens.generateToken("ana"));
        assertEquals("ana", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test void tokensAreNeverLoggedAtAnyLevel() throws Exception {
        var valid = tokens.generateToken("ana");
        run("Bearer " + valid);
        SecurityContextHolder.clearContext();
        run("Bearer " + valid.substring(0, valid.length() - 4) + "AAAA");
        run(null);

        assertFalse(appender.list.isEmpty(), "el filtro sí registra eventos (en DEBUG)");
        var signature = valid.substring(valid.lastIndexOf('.') + 1);
        for (var event : appender.list) {
            assertFalse(event.getFormattedMessage().contains(valid), event.getFormattedMessage());
            assertFalse(event.getFormattedMessage().contains(signature), event.getFormattedMessage());
        }
    }

    @Test void validationDetailsAreNotLoggedAtInfo() throws Exception {
        run("Bearer " + tokens.generateToken("ana"));
        run(null);
        assertTrue(appender.list.stream().noneMatch(e -> e.getLevel() == Level.INFO),
                "la validación por petición se registra en DEBUG, no en INFO");
    }
}
