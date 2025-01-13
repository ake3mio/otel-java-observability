package com.ake3m.pocs.telemetry;

public record TelemetryProperties(
      String pyroscopeEndpoint,
      String tempoEndpoint,
      String applicationName
) {
}
