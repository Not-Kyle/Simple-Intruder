package net.mawborne.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.mawborne.UserData;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Duration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Presence {
    private static final Logger LOGGER = LogManager.getLogger(Presence.class);
    private static final String PRESENCE_API_URL = "https://presence.roblox.com/v1/presence/users";

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String BLUE = "\u001B[34m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";

    private final HttpClient client;
    private final ObjectMapper mapper;

    public Presence(HttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public CompletableFuture<UserData> getUserPresence(String cookie, UserData currentData) {
        return sendRequest(cookie, null, currentData);
    }

    private CompletableFuture<UserData> sendRequest(String cookie, String csrfToken, UserData currentData) {
        try {
            String requestBody = mapper.writeValueAsString(Map.of("userIds", List.of(currentData.userId())));

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(PRESENCE_API_URL))
                    .timeout(Duration.ofSeconds(5))
                    .header("Cookie", ".ROBLOSECURITY=" + cookie)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json")
                    .header("Referer", "https://www.roblox.com/settings/account")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            if (csrfToken != null) {
                builder.header("X-CSRF-TOKEN", csrfToken);
            }

            return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenCompose(response -> {
                        if (response.statusCode() == 403 && csrfToken == null) {
                            String newToken = response.headers().firstValue("x-csrf-token").orElse(null);
                            if (newToken != null) {
                                return sendRequest(cookie, newToken, currentData);
                            }
                        }
                        return CompletableFuture.completedFuture(processResponse(response, currentData));
                    });

        } catch (Exception e) {
            LOGGER.error("Presence request failed: {}", e.getMessage());
            return CompletableFuture.completedFuture(currentData);
        }
    }

    private UserData processResponse(HttpResponse<String> response, UserData currentData) {
        if (response.statusCode() != 200) {
            LOGGER.error("API Error {}: {}", response.statusCode(), response.body());
            return currentData;
        }

        try {
            JsonNode json = mapper.readTree(response.body()).path("userPresences").path(0);

            if (json.isMissingNode()) {
                return currentData.withCurrentStatus(statusMap(0))
                        .withLastLocation("N/A")
                        .withGameId("N/A")
                        .withPlaceId(0)
                        .withUniverseId(0);
            }

            String location = json.path("lastLocation").asText();
            if (location.isBlank()) location = "Private or Hidden";

            LOGGER.info("[SUCCESS] Processed response from: {}", response.uri());

            return currentData.withCurrentStatus(statusMap(json.path("userPresenceType").asInt()))
                    .withLastLocation(location)
                    .withGameId(json.path("gameId").asText())
                    .withPlaceId(json.path("placeId").asInt())
                    .withUniverseId(json.path("universeId").asInt());

        } catch (JsonProcessingException err) {
            LOGGER.error("[FAILURE] Failed to parse user JSON: {}", err.getMessage());
            return currentData;
        }
    }

    private String statusMap(int type) {
        return switch (type) {
            case 0 -> RED + "Offline" + RESET;
            case 1 -> BLUE + "Online" + RESET;
            case 2 -> GREEN + "In‑Game" + RESET;
            case 3 -> YELLOW + "Studio" + RESET;
            default -> RESET + "Unknown";
        };
    }
}