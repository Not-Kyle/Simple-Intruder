package net.mawborne.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import net.mawborne.UserData;

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

public class Email {
    private static final Logger LOGGER = LogManager.getLogger(Email.class);
    private static final String EMAIL_API_URL = "https://accountsettings.roblox.com/v1/email";
    private static final String EMAILS_API_URL = "https://accountsettings.roblox.com/v1/emails";

    private final HttpClient client;
    private final ObjectMapper mapper;

    public Email(HttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public CompletableFuture<UserData> getUserEmail(String cookie, UserData currentUser) {
        return sendRequest(EMAIL_API_URL, cookie, currentUser, this::processResponse);
    }

    public CompletableFuture<UserData> getUserEmails(String cookie, UserData currentUser) {
        return sendRequest(EMAILS_API_URL, cookie, currentUser, this::processResponse);
    }

    private CompletableFuture<UserData> sendRequest(String url, String cookie, UserData currentData, BiFunction<HttpResponse<String>, UserData, UserData> processor) {
        if (cookie == null || cookie.isBlank()) { // Rejects the entire class as you can not use Email API without a cookie.
            LOGGER.warn("Attempted API call without a valid cookie.");

            return CompletableFuture.completedFuture(currentData.withCurrentEmail("N/A (No Cookie)")
                    .withPendingEmail("N/A (No Cookie)"));
        }

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

            if (root.has("emailAddress")) {
                updatedData = updatedData.withCurrentEmail(root.path("emailAddress").asText())
                        .withVerified(root.path("verified").asBoolean())
                        .withCanBypass(root.path("canBypassPasswordForEmailUpdate").asBoolean());
            }

            if (root.has("pendingEmail")) {
                updatedData = updatedData.withPendingEmail(root.get("pendingEmail").asText());
            }

            LOGGER.info("[SUCCESS] Processed response from: {}", response.uri());
            return updatedData;

        } catch (JsonProcessingException err) {
            LOGGER.error("[FAILURE] Failed to parse user JSON: {}", err.getMessage());
            return currentData;
        }
    }
}
