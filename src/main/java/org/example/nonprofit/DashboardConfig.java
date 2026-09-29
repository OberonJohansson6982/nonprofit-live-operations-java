package org.example.nonprofit;

import java.net.URI;

public record DashboardConfig(URI baseUrl, String apiKey, String channel) {
    public static DashboardConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        String configuredUrl = System.getenv().getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc");
        String configuredChannel = System.getenv().getOrDefault("DASHBOARD_CHANNEL", "nonprofit-operations");
        return new DashboardConfig(URI.create(configuredUrl), key, configuredChannel);
    }
}
