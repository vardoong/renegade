package com.renegade.telemetry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class TelemetryMetrics {

    private final Counter successCounter;
    private final Counter failureCounter;

    public TelemetryMetrics(MeterRegistry registry) {
        successCounter = Counter.builder("telemetry.save")
                .description("Telemetry persistence outcomes")
                .tag("result", "success")
                .register(registry);

        failureCounter = Counter.builder("telemetry.save")
                .description("Telemetry persistence outcomes")
                .tag("result", "failure")
                .register(registry);
    }

    public void recordSuccess() {
        successCounter.increment();
    }

    public void recordFailure() {
        failureCounter.increment();
    }
}