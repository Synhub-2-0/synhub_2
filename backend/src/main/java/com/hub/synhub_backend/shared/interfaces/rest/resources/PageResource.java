package com.hub.synhub_backend.shared.interfaces.rest.resources;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Una página de resultados (AUDIT PERF-02). Solo se devuelve cuando el cliente pide {@code ?page} o {@code ?size}. */
public record PageResource<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <E, T> PageResource<T> of(Page<E> page, Function<E, T> toResource) {
        return new PageResource<>(page.getContent().stream().map(toResource).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
