package com.ake3m.pocs.telemetry;

import io.micrometer.context.ContextRegistry;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.micrometer.tracing.contextpropagation.ObservationAwareBaggageThreadLocalAccessor;
import io.micrometer.tracing.contextpropagation.ObservationAwareSpanThreadLocalAccessor;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.pyroscope.http.Format;
import io.pyroscope.javaagent.EventType;
import io.pyroscope.javaagent.PyroscopeAgent;
import io.pyroscope.javaagent.config.Config;

public class TelemetryConfiguration {

    private final MetricProvider metricProvider;
    private final TracerProvider tracerProvider;

    public TelemetryConfiguration(TelemetryProperties properties) {
        PyroscopeAgent.start(
                new Config.Builder()
                        .setApplicationName(properties.applicationName())
                        .setProfilingEvent(EventType.ITIMER)
                        .setProfilingAlloc("512k")
                        .setProfilingLock("10ms")
                        .setFormat(Format.JFR)
                        .setServerAddress(properties.pyroscopeEndpoint())
                        .build()
        );
        tracerProvider = new TracerProvider(properties);
        metricProvider = new MetricProvider(properties);
    }

    public static TelemetryConfiguration create(TelemetryProperties properties) {
        return new TelemetryConfiguration(properties);
    }

    public PrometheusMeterRegistry getMeterRegistry() {
        return metricProvider.getMeterRegistry();
    }

    public Telemetry getTelemetry() {
        return new Telemetry(tracerProvider.getTracer());
    }

    public ObservationRegistry getObservationRegistry() {
        OtelPropagator propagator = new OtelPropagator(ContextPropagators.create(B3Propagator.injectingSingleHeader()), tracerProvider.getOtelTracer());

        ObservationRegistry observationRegistry = ObservationRegistry.create();

        observationRegistry.observationConfig()
                           .observationHandler(new DefaultMeterObservationHandler(metricProvider.getMeterRegistry()))
                           .observationHandler(new ObservationHandler.FirstMatchingCompositeObservationHandler(
                                   new PropagatingSenderTracingObservationHandler<>(tracerProvider.getTracer(), propagator),
                                   new PropagatingReceiverTracingObservationHandler<>(tracerProvider.getTracer(), propagator),
                                   new DefaultTracingObservationHandler(tracerProvider.getTracer())));

        ContextRegistry.getInstance()
                       .registerThreadLocalAccessor(new ObservationAwareSpanThreadLocalAccessor(tracerProvider.getTracer()))
                       .registerThreadLocalAccessor(new ObservationAwareBaggageThreadLocalAccessor(observationRegistry, tracerProvider.getTracer()));

        return observationRegistry;
    }
}
