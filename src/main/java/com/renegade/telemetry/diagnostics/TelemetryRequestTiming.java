package com.renegade.telemetry.diagnostics;

import net.ttddyy.dsproxy.ConnectionInfo;

import java.util.IdentityHashMap;
import java.util.Map;

final class TelemetryRequestTiming {
    enum Phase {
        ACQUIRE, SQL, COMMIT
    }

    private static final ThreadLocal<TelemetryRequestTiming> CURRENT = new ThreadLocal<>();

    private final long startedAt = System.nanoTime();
    private final Map<ConnectionInfo, Long> borrowedAt = new IdentityHashMap<>(1);
    private long acquireNanos;
    private long sqlNanos;
    private long commitNanos;
    private long holdNanos;

    static TelemetryRequestTiming start() {
        TelemetryRequestTiming timing = new TelemetryRequestTiming();
        CURRENT.set(timing);
        return timing;
    }

    static TelemetryRequestTiming current() {
        return CURRENT.get();
    }

    static void clear() {
        CURRENT.remove();
    }

    void cRecord(Phase phase, long nanos) {
        switch (phase) {
            case ACQUIRE -> acquireNanos += nanos;
            case SQL -> sqlNanos += nanos;
            case COMMIT -> commitNanos += nanos;
        }
    }

    void connectionBorrowed(ConnectionInfo connection, long now) {
        borrowedAt.put(connection, now);
    }

    void connectionReturned(ConnectionInfo connection, long now) {
        Long borrowed = borrowedAt.remove(connection);
        if (borrowed != null) {
            holdNanos += now - borrowed;
        }
    }

    long elapsedNanos() {
        return System.nanoTime() - startedAt;
    }

    double acquireMillis() {
        return acquireNanos / 1_000_000.0;
    }

    double sqlMillis() {
        return sqlNanos / 1_000_000.0;
    }

    double commitMillis() {
        return commitNanos / 1_000_000.0;
    }

    double holdMillis() {
        return holdNanos / 1_000_000.0;
    }
}
