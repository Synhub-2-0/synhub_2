package com.hub.synhub_backend.shared.interfaces.rest;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Paginación opcional de los listados (AUDIT PERF-02): sin {@code page} ni {@code size} la respuesta sigue siendo
 * un arreglo completo, para no romper a los clientes actuales.
 */
public final class Pagination {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private Pagination() {
    }

    /** Vacío si no se pidió paginación. {@code page} empieza en 0; {@code size} se limita a {@value #MAX_SIZE}. */
    public static Optional<Pageable> of(Integer page, Integer size) {
        if (page == null && size == null) return Optional.empty();
        var pageNumber = page == null ? 0 : page;
        var pageSize = size == null ? DEFAULT_SIZE : size;
        if (pageNumber < 0) throw new IllegalArgumentException("page must be 0 or greater");
        if (pageSize < 1) throw new IllegalArgumentException("size must be 1 or greater");
        return Optional.of(PageRequest.of(pageNumber, Math.min(pageSize, MAX_SIZE)));
    }
}
