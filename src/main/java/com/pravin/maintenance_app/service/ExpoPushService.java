package com.pravin.maintenance_app.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ExpoPushService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    private final RestClient restClient;

    public ExpoPushService() {
        this.restClient = RestClient.builder()
                .baseUrl(EXPO_PUSH_URL)
                .build();
    }

    public void sendPushNotificationsAsync(Collection<String> pushTokens, String title, String body, Map<String, Object> data) {
        if (pushTokens == null || pushTokens.isEmpty()) {
            log.info("No push tokens provided for notification: {}", title);
            return;
        }

        // Filter valid Expo tokens
        List<String> validTokens = pushTokens.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(t -> t.startsWith("ExponentPushToken[") || t.startsWith("ExpoPushToken["))
                .distinct()
                .toList();

        if (validTokens.isEmpty()) {
            log.info("No valid Expo push tokens found among provided tokens: {}", pushTokens);
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                // Chunk into batches of up to 100 as recommended by Expo
                int batchSize = 100;
                for (int i = 0; i < validTokens.size(); i += batchSize) {
                    List<String> batch = validTokens.subList(i, Math.min(i + batchSize, validTokens.size()));
                    List<Map<String, Object>> messages = new ArrayList<>();

                    for (String token : batch) {
                        Map<String, Object> message = new HashMap<>();
                        message.put("to", token);
                        message.put("sound", "default");
                        message.put("title", title);
                        message.put("body", body);
                        message.put("priority", "high");
                        message.put("channelId", "default");
                        if (data != null && !data.isEmpty()) {
                            message.put("data", data);
                        }
                        messages.add(message);
                    }

                    String response = restClient.post()
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header("Accept-Encoding", "gzip, deflate")
                            .body(messages)
                            .retrieve()
                            .body(String.class);

                    log.info("Expo push notification dispatched successfully to {} devices. Response: {}", batch.size(), response);
                }
            } catch (Exception ex) {
                log.error("Failed to send Expo push notifications for title '{}': {}", title, ex.getMessage(), ex);
            }
        });
    }
}

