package net.mawborne.api;

import net.mawborne.UserData;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class Users {
    private static final Logger LOGGER = LogManager.getLogger(Users.class);
    private static final String USERS_API = "https://users.roblox.com/v1/users/";
    private static final String COUNTRYCODE_API = "https://users.roblox.com/v1/users/authenticated/country-code";
    private static final String AGEBRACKET_API = "https://users.roblox.com/v1/users/authenticated/age-bracket";

    private final HttpClient client;
    private final ObjectMapper mapper;

    public Users(HttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public CompletableFuture<UserData> getUserData(String cookie, UserData currentData) {
        return sendRequest(USERS_API + currentData.userId(), cookie, currentData, this::processResponse);
    }

    public CompletableFuture<UserData> getCountryCode(String cookie, UserData currentData) {
        if (cookie == null || cookie.isBlank()) {
            return CompletableFuture.completedFuture(currentData);
        }

        return sendRequest(COUNTRYCODE_API, cookie, currentData, this::processResponse);
    }

    public CompletableFuture<UserData> getAgeBracket(String cookie, UserData currentData) {
        if (cookie == null || cookie.isBlank()) {
            return CompletableFuture.completedFuture(currentData);
        }

        return sendRequest(AGEBRACKET_API, cookie, currentData, this::processResponse);
    }

    private CompletableFuture<UserData> sendRequest(String url, String cookie, UserData currentData, BiFunction<HttpResponse<String>, UserData, UserData> processor) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Cookie", ".ROBLOSECURITY=" + cookie)
                .header("Accept", "application/json")
                .header("Referer", "https://www.roblox.com/settings/account")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> processor.apply(response, currentData))
                .exceptionally(ex -> {
                    LOGGER.error("API Connection Error at {}: {}", url, ex.getMessage());
                    return currentData;
                });
    }

    private UserData processResponse(HttpResponse<String> response, UserData currentData) {
        if (response.statusCode() != 200) {
            LOGGER.error("API Error {}: {}", response.statusCode(), response.body());
            return currentData;
        }

        try {
            JsonNode root = mapper.readTree(response.body());
            UserData updatedData = currentData;

            if (root.has("created")) {
                updatedData = updatedData.withUsername(root.path("name").asText())
                        .withDisplayName(root.path("displayName").asText())
                        .withUserId(root.path("id").asLong())
                        .withBanned(root.path("isBanned").asBoolean())
                        .withDescription(root.path("description").asText())
                        .withHasVerifiedBadge(root.path("hasVerifiedBadge").asBoolean())
                        .withCreated(root.path("created").asText());

            } else if(root.has("countryCode")) {
                updatedData = updatedData.withCountryCode(root.path("countryCode").asText());

            } else if(root.has("ageBracket")) {
                updatedData = updatedData.withAgeBracket(ageBracketId(root.path("ageBracket").asInt()));
            }

            LOGGER.info("[SUCCESS] Processed response from: {}", response.uri());
            return updatedData;

        } catch (JsonProcessingException err) {
            LOGGER.error("[FAILURE] Failed to parse user JSON: {}", err.getMessage());
            return currentData;
        }
    }

    private String ageBracketId(int id) {
        return switch (id) {
            case 1 -> "Under 9";
            case 2 -> "9-12";
            case 3 -> "13-15";
            case 4 -> "16-17";
            case 5 -> "18-20";
            case 6 -> "21+";
            default -> "Undefined/Unknown";
        };
    }
}