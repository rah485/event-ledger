package com.eventledger.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Counter eventCounter;

    public MetricsService(MeterRegistry meterRegistry) {

        this.eventCounter = Counter.builder("gateway.events.created")
                .description("Number of events created successfully")
                .register(meterRegistry);
    }

    /**
     * Increment custom event counter.
     */
    public void incrementEventCounter() {
        eventCounter.increment();
    }

}