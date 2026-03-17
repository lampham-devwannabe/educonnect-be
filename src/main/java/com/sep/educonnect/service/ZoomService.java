package com.sep.educonnect.service;

import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import com.sep.educonnect.dto.zoom.ZoomMeetingRequest;
import com.sep.educonnect.dto.zoom.ZoomMeetingResponse;
import com.sep.educonnect.entity.ClassSession;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.ClassSessionRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class ZoomService {

    @Value("${zoom.account-id}")
    String accountId;

    @Value("${zoom.client-id}")
    String clientId;

    @Value("${zoom.client-secret}")
    String clientSecret;

    final ClassSessionRepository classSessionRepository;

    public String getAccessToken() {
        try {
            String auth = Base64.getEncoder()
                    .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create("https://zoom.us/oauth/token?grant_type=account_credentials&account_id=" + accountId))
                    .header("Authorization", "Basic " + auth)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new AppException(ErrorCode.GET_MEETING_FAILED);
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String token = json.get("access_token").getAsString();
            log.info("Zoom access token retrieved successfully");
            return token;

        } catch (Exception e) {
            throw new RuntimeException("Error retrieving Zoom token: " + e.getMessage(), e);
        }
    }

    public ZoomMeetingResponse createMeeting(ZoomMeetingRequest req) {

        ClassSession classSession = classSessionRepository.findById(req.getSessionId())
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_EXISTED));

        try {
            String token = getAccessToken();

            JsonObject body = new JsonObject();
            body.addProperty("topic", req.getTopic());
            body.addProperty("type", 2); // 2 = scheduled meeting
            body.addProperty("start_time", req.getStartTime());
            body.addProperty("duration", req.getDuration());
            body.addProperty("timezone", "Asia/Ho_Chi_Minh");

            // ✅ Add password (Zoom will validate it)
            body.addProperty("password", req.getPassword() != null ? req.getPassword() : generateRandomPassword());

            JsonObject settings = new JsonObject();
            settings.addProperty("join_before_host", true);
            settings.addProperty("mute_upon_entry", true);
            settings.addProperty("waiting_room", false);
            settings.addProperty("approval_type", 0); // automatically approve
            body.add("settings", settings);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.zoom.us/v2/users/me/meetings"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                log.error("Failed to create Zoom meeting: {}", response.body());
                throw new RuntimeException("Zoom API error: " + response.body());
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            classSession.setTopic(json.get("topic").getAsString());
            classSession.setMeetingId(json.get("id").getAsString()); // fixed field name
            classSession.setMeetingJoinUrl(json.get("join_url").getAsString());
            classSession.setMeetingStartUrl(json.get("start_url").getAsString());
            classSession.setMeetingPassword(json.get("password").getAsString()); // ✅ save password

            classSessionRepository.save(classSession);

            return new ZoomMeetingResponse(
                    json.get("id").getAsString(),
                    json.get("join_url").getAsString(),
                    json.get("start_url").getAsString(),
                    json.get("topic").getAsString(),
                    json.get("start_time").getAsString(),
                    json.get("password").getAsString() // ✅ include password in response
            );

        } catch (Exception e) {
            log.error("Error creating Zoom meeting", e);
            throw new RuntimeException("Error creating Zoom meeting: " + e.getMessage(), e);
        }
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@-_";
        SecureRandom random = new SecureRandom();
        return random.ints(8, 0, chars.length())
                .mapToObj(chars::charAt)
                .map(Object::toString)
                .collect(Collectors.joining());
    }

}
