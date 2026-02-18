package net.mawborne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.With;

/* @JsonIgnoreProperties tells Jackson to ignore any extra data */

@With
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserData(
        @JsonProperty("name") String username,
        String displayName,
        String description,
        String gender,
        String created,
        String countryCode,
        String ageBracket,

        @JsonProperty("emailAddress") String currentEmail,
        @JsonProperty("verified") boolean isVerified,
        @JsonProperty("canBypassPasswordForEmailUpdate") boolean canBypass,
        String pendingEmail,

        @JsonProperty("prefix") String phonePrefix,
        String phone,
        @JsonProperty("isVerified") boolean phoneVerified,
        @JsonProperty("verificationCodeLength") int codeLength,

        @JsonProperty("userPresenceType") String currentStatus,
        String lastLocation,
        String gameId,
        int placeId,
        int universeId,

        long userId,
        boolean isBanned,
        boolean hasVerifiedBadge
) {

    public UserData {
        username = (username == null) ? "N/A" : username;
        displayName = (displayName == null) ? "N/A" : displayName;
        description = (description == null) ? "No description provided." : description;
        gender = (gender == null) ? "N/A" : gender;
        created = (created == null) ? "N/A" : created;
        countryCode = (countryCode == null) ? "N/A" : countryCode;
        ageBracket = (ageBracket == null) ? "N/A" : ageBracket;

        currentEmail = (currentEmail == null) ? "N/A" : currentEmail;
        pendingEmail = (pendingEmail == null) ? "N/A" : pendingEmail;

        phonePrefix = (phonePrefix == null) ? "N/A" : phonePrefix;
        phone = (phone == null) ? "N/A" : phone;

        currentStatus = (currentStatus == null) ? "\u001B[37mUnknown\u001B[0m" : currentStatus;
        lastLocation = (lastLocation == null) ? "Private or Hidden" : lastLocation;
        gameId = (gameId == null) ? "N/A" : gameId;
    }

    public static UserData createEmpty(long userId) {
        return new UserData(
                null, null, null, null, null, null, null,
                null, false, false, null,
                null, null, false, 0,
                null, null, null, 0, 0,
                userId, false, false
        );
    }

    @Override
    public String toString() {
        String border = "=".repeat(45);
        String divider = "-".repeat(45);

        // Apply color only during printing to keep the data clean
        String statusColor = currentStatus.equals("Unknown") ? "\u001B[37mUnknown\u001B[0m" : currentStatus;

        return String.format("""
            %n%s
             ROBLOX PROFILE DOSSIER
            %s
             %-15s : %s
             %-15s : %s
             %-15s : %s
             %-15s : %s
             %-15s : %s
             %-15s : %b
             %-15s : %b
            %s
             %-15s : %s
             %-15s : %s
             %-15s : %b
             %-15s : %d
            %s
             %-15s : %s
             %-15s : %s
             %-15s : %d
             %-15s : %s
             %-15s : %s
             %-15s : %s
             %-15s : %s
             %-15s : %s
             %-15s : %s
            %s""",
                border, divider,
                "Username", username,
                "Display Name", displayName,
                "Description", (description.length() > 30 ? description.substring(0, 27) + "..." : description),
                "Email", currentEmail,
                "Pending Email", pendingEmail,
                "Email Verified", isVerified,
                "Can Bypass", canBypass,
                divider,
                "Phone #", phone,
                "Phone Prefix", phonePrefix,
                "Phone Verified", phoneVerified,
                "Code Length", codeLength,
                divider,
                "Country Code", countryCode,
                "Age Bracket", ageBracket,
                "User ID", userId,
                "Status", statusColor,
                "Location", lastLocation,
                "Gender", gender,
                "Created", created,
                "Verified Badge", (hasVerifiedBadge ? "Yes" : "No"),
                "Banned", (isBanned ? "YES" : "No"),
                border
        );
    }
}

