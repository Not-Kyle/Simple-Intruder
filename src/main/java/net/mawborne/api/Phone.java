package net.mawborne.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import net.mawborne.UserData;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class Phone {
    private static final Logger LOGGER = LogManager.getLogger(Phone.class);
    private static final String PHONE_API_URL = "https://accountinformation.roblox.com/v1/phone";

    private final HttpClient client;
    private final ObjectMapper mapper;

    public Phone(HttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public CompletableFuture<UserData> getUserPhone(String cookie, UserData currentUser) {
        return sendRequest(cookie, currentUser);
    }

    private CompletableFuture<UserData> sendRequest(String cookie, UserData currentData) {
        if (cookie == null || cookie.isBlank()) { // Rejects the entire class as you can not use Phone API without a cookie.
            LOGGER.warn("Attempted API call without a valid cookie.");

            return CompletableFuture.completedFuture(currentData.withPhone("N/A (No Cookie)")
                    .withPhonePrefix("N/A (No Cookie)"));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PHONE_API_URL))
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
                    LOGGER.error("API Connection Error at {}: {}", PHONE_API_URL, ex.getMessage());
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

            LOGGER.info("[SUCCESS] Processed response from: {}", response.uri());
            return currentData.withPhone(root.path("phone").asText())
                    .withPhoneVerified(root.path("isVerified").asBoolean())
                    .withPhonePrefix(root.path("prefix").asText())
                    .withCodeLength(root.path("verificationCodeLength").asInt());

        } catch (JsonProcessingException err) {
            LOGGER.error("[FAILURE] Failed to parse user JSON: {}", err.getMessage());
            return currentData;
        }
    }
}
