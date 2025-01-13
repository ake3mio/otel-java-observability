package com.ake3m.pocs.telemetry;

import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics;
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmCompilationMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.tracer.otel.OpenTelemetrySpanContext;

import java.io.File;
import java.util.List;

class MetricProvider {
    private static final List<MeterBinder> METRICS = List.of(
            new ClassLoaderMetrics(),
            new DiskSpaceMetrics(new File(System.getProperty("user.dir"))),
            new JvmCompilationMetrics(),
            new JvmGcMetrics(),
            new JvmHeapPressureMetrics(),
            new JvmInfoMetrics(),
            new JvmMemoryMetrics(),
            new JvmThreadMetrics(),
            new UptimeMetrics(),
            new FileDescriptorMetrics(),
            new LogbackMetrics(),
            new ProcessMemoryMetrics(),
            new ProcessThreadMetrics(),
            new ProcessorMetrics());
    private final PrometheusMeterRegistry registry;

    public MetricProvider(TelemetryProperties properties) {
        PrometheusMeterRegistry registry = getPrometheusMeterRegistry(properties.applicationName());
        bindMetrics(registry);
        this.registry = registry;
    }

    private void bindMetrics(PrometheusMeterRegistry registry) {
        METRICS.forEach(meterBinder -> meterBinder.bindTo(registry));
    }

    private PrometheusMeterRegistry getPrometheusMeterRegistry(String applicationName) {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(
                PrometheusConfig.DEFAULT,
                new PrometheusRegistry(),
                Clock.SYSTEM,
                new OpenTelemetrySpanContext());
        registry.config()
                .commonTags("service_name", applicationName)
                .commonTags("language", "java");
        return registry;
    }

    public PrometheusMeterRegistry getMeterRegistry() {
        return registry;
    }
}
