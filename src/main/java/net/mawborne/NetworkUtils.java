package net.mawborne;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class NetworkUtils {
    private static final Logger LOGGER = LogManager.getLogger(NetworkUtils.class);
    private static final String IP_URL = "https://api.ipify.org";

    public static CompletableFuture<String> getPublicIPAsync(HttpClient client) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(IP_URL))
                .timeout(Duration.ofSeconds(5)) // Don't let the app hang forever
                .header("Accept", "text/plain")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return response.body();
                    }

                    LOGGER.warn("IP API returned status: {}", response.statusCode());
                    return "Unknown (HTTP " + response.statusCode() + ")";
                })

                .exceptionally(ex -> {
                    LOGGER.error("Async IP fetch failed: {}", ex.getMessage());
                    return "Connection Error";
                });
    }
}