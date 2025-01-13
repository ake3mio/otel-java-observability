package com.ake3m.pocs.telemetry;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.*;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.otel.pyroscope.OtelProfilerSdkBridge;
import io.otel.pyroscope.PyroscopeOtelConfiguration;
import io.otel.pyroscope.PyroscopeOtelSpanProcessor;

import java.time.Duration;
import java.util.Collections;

import static io.opentelemetry.sdk.trace.samplers.Sampler.alwaysOn;

class TracerProvider {

    private final io.opentelemetry.api.trace.Tracer otelTracer;
    private final Tracer tracer;
    private final OtelTracer.EventPublisher eventPublisher;
    private final TelemetryProperties properties;

    public TracerProvider(TelemetryProperties properties) {
        this.properties = properties;
        eventPublisher = getEventPublisher();

        ContextStorage.addWrapper(new EventPublishingContextWrapper(eventPublisher));

        this.otelTracer = createOtelTracer();
        this.tracer = createTracer();
    }

    private OtelTracer.EventPublisher getEventPublisher() {
        Slf4JEventListener slf4JEventListener = new Slf4JEventListener();
        Slf4JBaggageEventListener slf4JBaggageEventListener = new Slf4JBaggageEventListener(Collections.emptyList());

        return event -> {
            slf4JEventListener.onEvent(event);
            slf4JBaggageEventListener.onEvent(event);
        };
    }

    public Tracer getTracer() {
        return tracer;
    }

    public io.opentelemetry.api.trace.Tracer getOtelTracer() {
        return otelTracer;
    }

    private Tracer createTracer() {
        OtelCurrentTraceContext otelCurrentTraceContext = new OtelCurrentTraceContext();


        return new OtelTracer(otelTracer, otelCurrentTraceContext, eventPublisher, new OtelBaggageManager(otelCurrentTraceContext, Collections.emptyList(), Collections.emptyList()));
    }

    private io.opentelemetry.api.trace.Tracer createOtelTracer() {
        OpenTelemetrySdk openTelemetrySdk =
                OpenTelemetrySdk.builder()
                                .setTracerProvider(getSdkTracerProvider())
                                .setPropagators(ContextPropagators.create(B3Propagator.injectingSingleHeader()))
                                .build();

        GlobalOpenTelemetry.set(openTelemetrySdk);
        return openTelemetrySdk.getTracerProvider().get(properties.applicationName());
    }

    private SdkTracerProvider getSdkTracerProvider() {
        SpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                                                        .setEndpoint(properties.tempoEndpoint())
                                                        .setTimeout(Duration.ofSeconds(10))
                                                        .build();
        var resource = Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), properties.applicationName()));
        var batchSpanProcessor = BatchSpanProcessor.builder(spanExporter).build();
        return SdkTracerProvider.builder()
                                .setSampler(alwaysOn())
                                .addResource(resource)
                                .addSpanProcessor(batchSpanProcessor)
                                .addSpanProcessor(getPyroscopeOtelSpanProcessor())
                                .build();
    }

    private PyroscopeOtelSpanProcessor getPyroscopeOtelSpanProcessor() {
        PyroscopeOtelConfiguration pyroscopeOtelConfig = new PyroscopeOtelConfiguration.Builder()
                .setAppName(properties.applicationName())
                .setPyroscopeEndpoint(properties.pyroscopeEndpoint())
                .setAddProfileURL(true)
                .setAddSpanName(true)
                .setAddProfileBaselineURLs(true)
                .setRootSpanOnly(true)
                .build();
        return new PyroscopeOtelSpanProcessor(pyroscopeOtelConfig, loadProfilerSdk());
    }

    private static OtelProfilerSdkBridge loadProfilerSdk() {
        try {
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> sdkClass = systemClassLoader.loadClass("io.pyroscope.javaagent.ProfilerSdk");
            Object sdk = sdkClass.getDeclaredConstructor().newInstance();
            return new OtelProfilerSdkBridge(sdk);
        } catch (Exception e) {
            throw new RuntimeException("Error loading the profiler SDK", e);
        }
    }


}
