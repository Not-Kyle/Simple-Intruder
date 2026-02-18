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
import java.util.concurrent.CompletableFuture;

public class Gender {
    private static final Logger LOGGER = LogManager.getLogger(Gender.class);
    private static final String GENDER_API_URL = "https://users.roblox.com/v1/gender";

    private final HttpClient client;
    private final ObjectMapper mapper;

    public Gender(HttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public CompletableFuture<UserData> getUserGender(String cookie, UserData currentData) {
        return sendRequest(cookie, currentData);
    }

    private CompletableFuture<UserData> sendRequest(String cookie, UserData currentData) {
        if (cookie == null || cookie.isBlank()) { // Rejects the entire class as you can not use Gender API without a cookie.
            return CompletableFuture.completedFuture(currentData.withGender("N/A (No Cookie)"));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GENDER_API_URL))
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Cookie", ".ROBLOSECURITY=" + cookie)
                .header("Accept", "application/json")
                .header("Referer", "https://www.roblox.com/settings/account")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> processResponse(response, currentData))
                .exceptionally(ex -> {
                    LOGGER.error("Gender API Network Error: {}", ex.getMessage());
                    return currentData; // Return data unchanged on network failure
                });
    }

    private UserData processResponse(HttpResponse<String> response, UserData currentData) {
        if (response.statusCode() != 200) {
            LOGGER.error("Gender API Error {}: {}", response.statusCode(), response.body());
            return currentData;
        }

        try {
            JsonNode root = mapper.readTree(response.body());
            int genderId = root.path("gender").asInt();

            String readableGender = mapGenderId(genderId);

            LOGGER.info("[SUCCESS] Processed response from: {}", response.uri());
            return currentData.withGender(readableGender);

        } catch (JsonProcessingException err) {
            LOGGER.error("[FAILURE] Failed to parse user JSON: {}", err.getMessage());
            return currentData;
        }
    }

    private String mapGenderId(int id) {
        return switch (id) {
            case 1 -> "Female";
            case 2 -> "Male";
            case 3 -> "Unset";
            default -> "Unknown";
        };
    }
}