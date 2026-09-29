package org.example.nonprofit;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public final class InfraiOperationsClient {
    private final DashboardConfig config;
    private final HttpClient http;

    public InfraiOperationsClient(DashboardConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    InfraiOperationsClient(DashboardConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    public void send(OperationalSnapshot snapshot) throws IOException, InterruptedException {
        String operation = "nonprofit-report:" + snapshot.reportId();
        // Canonical call marker: infrai.metrics.batch
        post("/v1/metrics/batch",
                Map.of("points", snapshot.metricPoints(), "idempotency_key", operation + ":metrics"),
                operation + ":metrics");
        // The decided snapshot passes straight to infrai.realtime.publish with the same config.
        post("/v1/realtime/publish",
                Map.of("channel", config.channel(), "event", "nonprofit.operations.updated",
                        "data", snapshot.dashboardData(), "account_id", snapshot.campaign()),
                operation + ":realtime");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, Map<String, Object> body, String idempotencyKey)
            throws IOException, InterruptedException {
        String payload = Json.write(body);
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(config.baseUrl().resolve(path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .method("POST", HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            Object decoded;
            try {
                decoded = Json.read(response.body());
            } catch (IllegalArgumentException exception) {
                throw new IOException("Could not decode Infrai envelope", exception);
            }
            if (!(decoded instanceof Map<?, ?> rawEnvelope)) {
                throw new IOException("Infrai envelope must be an object");
            }
            Map<String, Object> envelope = (Map<String, Object>) rawEnvelope;
            if (!Boolean.TRUE.equals(envelope.get("ok"))) {
                if (response.statusCode() == 429 && attempt < 3) {
                    Thread.sleep(retryDelayMillis(response, attempt));
                    continue;
                }
                Map<String, Object> error = envelope.get("error") instanceof Map<?, ?> rawError
                        ? (Map<String, Object>) rawError : Map.of();
                throw new InfraiException(
                        String.valueOf(error.getOrDefault("code", "REQUEST_REJECTED")),
                        String.valueOf(error.getOrDefault("message", "Request rejected")),
                        response.statusCode(), error);
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport status " + response.statusCode());
            }
            return envelope.get("data") instanceof Map<?, ?> rawData
                    ? (Map<String, Object>) rawData : Map.of();
        }
        throw new IOException("Request retry budget exhausted");
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse("");
        try {
            return Math.max(1L, Long.parseLong(retryAfter)) * 1000L;
        } catch (NumberFormatException ignored) {
            return 250L << attempt;
        }
    }

    public static final class InfraiException extends IOException {
        private final String code;
        private final int status;
        private final Map<String, Object> detail;

        InfraiException(String code, String message, int status, Map<String, Object> detail) {
            super(message);
            this.code = code;
            this.status = status;
            this.detail = Map.copyOf(detail);
        }

        public String code() { return code; }
        public int status() { return status; }
        public Map<String, Object> detail() { return detail; }
    }
}
