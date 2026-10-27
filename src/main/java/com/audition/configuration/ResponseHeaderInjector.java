package com.audition.configuration;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds the current trace and span IDs to every response so callers can quote them when reporting a problem and support
 * can find the request in the logs. Uses Micrometer's Tracer, so it works with any tracing bridge.
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // after Spring Boot's observation filter, which starts the span
public class ResponseHeaderInjector extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String SPAN_ID_HEADER = "X-Span-Id";

    private final ObjectProvider<Tracer> tracerProvider;

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
        final FilterChain filterChain) throws ServletException, IOException {

        // Headers must be set before the chain runs, because the response may be committed afterwards.
        final Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer != null) {
            final Span span = tracer.currentSpan();
            if (span != null) {
                response.setHeader(TRACE_ID_HEADER, span.context().traceId());
                response.setHeader(SPAN_ID_HEADER, span.context().spanId());
            }
        }
        filterChain.doFilter(request, response);
    }

}
