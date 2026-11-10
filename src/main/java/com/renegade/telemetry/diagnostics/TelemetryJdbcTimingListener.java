package com.renegade.telemetry.diagnostics;

import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;

import java.sql.Connection;
import java.util.Objects;

final class TelemetryJdbcTimingListener implements MethodExecutionListener {
    private static final String STARTED_AT = TelemetryJdbcTimingListener.class.getName();

    @Override
    public void beforeMethod(MethodExecutionContext context) {
        if (phase(context) != null) {
            context.addCustomValue(STARTED_AT, System.nanoTime());
        }
    }

    @Override
    public void afterMethod(MethodExecutionContext context) {
        TelemetryRequestTiming timing = TelemetryRequestTiming.current();
        if (timing == null) {
            return;
        }

        Long startedAt = context.getCustomValue(STARTED_AT, Long.class);
        ConnectionInfo connection = context.getConnectionInfo();
        if (startedAt != null) {
            long now = System.nanoTime();
            TelemetryRequestTiming.Phase phase = Objects.requireNonNull(phase(context));
            timing.cRecord(phase, now - startedAt);
            if (phase == TelemetryRequestTiming.Phase.ACQUIRE && connection != null && context.getThrown() == null) {
                timing.connectionBorrowed(connection, now);
            }
        } else if (connection != null && context.getThrown() == null
                && "close".equals(context.getMethod().getName()) && context.getTarget() instanceof Connection) {
            timing.connectionReturned(connection, System.nanoTime());
        }
    }

    private static TelemetryRequestTiming.Phase phase(MethodExecutionContext context) {
        return switch (context.getMethod().getName()) {
            case "getConnection" -> TelemetryRequestTiming.Phase.ACQUIRE;
            case "execute",
                 "executeQuery",
                 "executeUpdate",
                 "executeLargeUpdate",
                 "executeBatch",
                 "executeLargeBatch" -> TelemetryRequestTiming.Phase.SQL;
            case "commit" -> TelemetryRequestTiming.Phase.COMMIT;
            default -> null;
        };
    }
}
