package com.renegade.telemetry.diagnostics;

import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;

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
        Long startedAt = context.getCustomValue(STARTED_AT, Long.class);
        if (startedAt != null) {
            TelemetryRequestTiming.current()
                    .cRecord(Objects.requireNonNull(phase(context)), System.nanoTime() - startedAt);
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
