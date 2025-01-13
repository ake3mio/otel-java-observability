package com.ake3m.pocs.telemetry;

import com.ake3m.pocs.entity.Traceable;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.pyroscope.labels.LabelsSet;
import io.pyroscope.labels.Pyroscope;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class Telemetry {
    private static final Logger log = LoggerFactory.getLogger(Telemetry.class);
    private final Tracer tracer;

    public Telemetry(Tracer tracer) {
        this.tracer = tracer;
    }

    public <T> T profile(String taskName, Callable<T> callable) {
        try {
            return Pyroscope.LabelsWrapper.run(new LabelsSet("task", taskName), callable);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ExecutorService wrap(ExecutorService executor) {
        return Context.taskWrapping(executor);
    }

    public void profile(String taskName, Runnable runnable) {
        try {
            Pyroscope.LabelsWrapper.run(new LabelsSet("task", taskName), runnable);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void traceProducer(String name, String serviceName, String destinationName, Runnable runnable) {
        //https://opentelemetry.io/docs/specs/semconv/messaging/messaging-spans/
        var builder = tracer.spanBuilder()
                            .kind(Span.Kind.PRODUCER)
                            .name(name)
                            .tag("service.name", serviceName)
                            .tag("messaging.system", "activemq")
                            .tag("messaging.operation.name", "send")
                            .tag("messaging.destination.name", destinationName);

        Optional
                .ofNullable(tracer.currentSpan())
                .map(Span::context)
                .ifPresent(builder::setParent);

        Span start = builder.start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(start)) {
            runnable.run();

        } catch (Exception e) {
            start.error(e);
            throw e;

        } finally {
            start.end();
        }
    }

    public void traceConsumer(String name, String destinationName, Runnable runnable) {
        //https://opentelemetry.io/docs/specs/semconv/messaging/messaging-spans/
        var builder = tracer.spanBuilder()
                            .kind(Span.Kind.CONSUMER)
                            .name(name)
                            .tag("messaging.system", "activemq")
                            .tag("messaging.operation.name", "ack")
                            .tag("messaging.destination.name", destinationName);

        Optional
                .ofNullable(tracer.currentSpan())
                .map(Span::context)
                .ifPresent(builder::setParent);

        Span start = builder.start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(start)) {
            runnable.run();

        } catch (Exception e) {
            start.error(e);
            throw e;

        } finally {
            start.end();
        }
    }

    public void trace(String name, Runnable runnable) {
        var span = tracer.nextSpan(tracer.currentSpan()).name(name);


        try (Tracer.SpanInScope ignored = tracer.withSpan(span.start())) {

            runnable.run();

        } catch (Exception e) {
            span.error(e);
            throw e;

        } finally {
            span.end();
        }
    }

    public <T> T trace(String name, Callable<T> callable) throws Exception {
        var span = tracer.nextSpan(tracer.currentSpan()).name(name);

        try (Tracer.SpanInScope ignored = tracer.withSpan(span.start())) {

            return callable.call();

        } catch (Exception e) {
            span.error(e);
            throw e;

        } finally {
            span.end();
        }
    }

    public void addSpanEvent(String info) {
        io.opentelemetry.api.trace.Span.current().addEvent(info, Instant.now());
    }

    public void addSpanError(String info, Exception exception) {
        io.opentelemetry.api.trace.Span.current().recordException(
                exception,
                Attributes.of(AttributeKey.stringKey("info"), info)
        );
    }

    public void injectTraceContext(Message message) {
        GlobalOpenTelemetry.getPropagators()
                           .getTextMapPropagator()
                           .inject(Context.current(), message, this::propagate);
    }

    public void usingTraceContext(Message message, Runnable runnable) {
        var context = extractTraceContext(message);

        try (var ignored = context.makeCurrent()) {
            runnable.run();
        }
    }

    public void usingTraceContext(Traceable<?> traceable, Runnable runnable) {
        var context = extractTraceContext(traceable);

        try (var ignored = context.makeCurrent()) {
            runnable.run();
        }
    }

    public <T> Traceable<T> toTraceable(T value) {
        var spanContext = io.opentelemetry.api.trace.Span.current().getSpanContext();
        var traceparent = "%s-%s-%s".formatted(
                spanContext.getTraceId(),
                spanContext.getSpanId(),
                spanContext.getTraceFlags().isSampled() ? 1 : 0
        );
        return new Traceable<>(traceparent, value);
    }

    private void propagate(Message message, String key, String value) {
        if (message != null) {
            try {
                message.setStringProperty(key, value);
            } catch (JMSException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private Context extractTraceContext(Traceable<?> traceable) {
        var b3Header = "b3";
        return GlobalOpenTelemetry.getPropagators()
                                  .getTextMapPropagator()
                                  .extract(Context.current(), Map.of(b3Header, traceable.traceid()), new TextMapGetter<>() {

                                      @Override
                                      public Iterable<String> keys(Map<String, String> carrier) {
                                          return List.of(b3Header);
                                      }

                                      @Override
                                      public String get(Map<String, String> carrier, String key) {
                                          return carrier.get(b3Header);
                                      }
                                  });
    }

    private static Context extractTraceContext(Message message) {
        return GlobalOpenTelemetry.getPropagators()
                                  .getTextMapPropagator()
                                  .extract(Context.current(), message, new TextMapGetter<>() {
                                      @Override
                                      public Iterable<String> keys(Message carrier) {
                                          try {
                                              return () -> {
                                                  try {
                                                      return carrier.getPropertyNames().asIterator();
                                                  } catch (JMSException e) {
                                                      log.warn(e.getMessage(), e);
                                                      throw new RuntimeException(e);
                                                  }
                                              };
                                          } catch (Exception e) {
                                              return List.of();
                                          }
                                      }

                                      @Override
                                      public String get(Message carrier, String s) {
                                          try {
                                              return carrier.getStringProperty(s);
                                          } catch (JMSException e) {
                                              log.warn(e.getMessage(), e);
                                              return null;
                                          }
                                      }
                                  });
    }
}
