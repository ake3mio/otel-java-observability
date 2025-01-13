package com.ake3m.pocs.delivery.http;

import com.ake3m.pocs.domain.TemperatureClient;
import com.ake3m.pocs.telemetry.Telemetry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.binder.httpcomponents.hc5.ObservationExecChainHandler;
import io.micrometer.observation.ObservationRegistry;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.ChainElement;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class HttpConfiguration {
    private final ObservationRegistry observationRegistry;
    private final Telemetry telemetry;
    private final ObjectMapper objectMapper;

    public HttpConfiguration(ObservationRegistry observationRegistry, Telemetry telemetry, ObjectMapper objectMapper) {
        this.observationRegistry = observationRegistry;
        this.telemetry = telemetry;
        this.objectMapper = objectMapper;
    }

    public TemperatureClient getTemperatureClient() {
        return new HttpTemperatureClient(objectMapper,
                new HttpResponseMapper(),
                getHttpClient(observationRegistry),
                "https://api.open-meteo.com/v1/forecast",
                telemetry);
    }

    private HttpClient getHttpClient(ObservationRegistry observationRegistry) {
        return HttpClients.custom()
                .setRetryStrategy(new DefaultHttpRequestRetryStrategy(3, TimeValue.of(Duration.ofSeconds(1))))
                .addExecInterceptorAfter(ChainElement.RETRY.name(), ObservationExecChainHandler.class.getName(), new ObservationExecChainHandler(observationRegistry))
                .setConnectionManager(getConnectionManager())
                .build();
    }

    private PoolingHttpClientConnectionManager getConnectionManager() {
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoKeepAlive(true)
                .build();
        return PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultSocketConfig(socketConfig)
                .setDefaultConnectionConfig(getConnectionConfig())
                .build();
    }

    private ConnectionConfig getConnectionConfig() {
        return ConnectionConfig.custom()
                .setConnectTimeout(30, TimeUnit.SECONDS)
                .setSocketTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}
