package com.renegade.telemetry.diagnostics;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

final class TelemetryTimingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(TelemetryTimingFilter.class);
    private final long thresholdNanos;

    TelemetryTimingFilter(long thresholdMillis) {
        this.thresholdNanos = TimeUnit.MILLISECONDS.toNanos(thresholdMillis);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
                || !request.getRequestURI().equals(request.getContextPath() + "/api/v1/telemetry");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        TelemetryRequestTiming timing = TelemetryRequestTiming.start();
        boolean completed = false;
        try {
            chain.doFilter(request, response);
            completed = true;
        } finally {
            TelemetryRequestTiming.clear();
            long totalNanos = timing.elapsedNanos();
            if (totalNanos >= thresholdNanos) {
                log.info("telemetry timing requestId={} status={} completed={} totalMs={} acquireMs={} sqlMs={} commitMs={}",
                        UUID.randomUUID(), response.getStatus(), completed, totalNanos / 1_000_000.0,
                        timing.acquireMillis(), timing.sqlMillis(), timing.commitMillis());
            }
        }
    }
}
