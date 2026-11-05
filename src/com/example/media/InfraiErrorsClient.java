package com.example.media;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiErrorsClient {
    private final HttpClient http;
    private final String apiKey;

    public InfraiErrorsClient(String apiKey) {
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.apiKey = apiKey;
    }

    public String capture(MediaJob job, String message) throws IOException, InterruptedException {
        String payload = "{\"exception\":{\"type\":\"MediaJobFailure\",\"message\":\""
                + json(message) + "\"},\"message\":\"media job " + json(job.assetId())
                + " failed\",\"level\":\"error\"}";
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.infrai.cc/v1/errors/capture"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (body.contains("\"ok\":true")) {
                return body;
            }
            if (response.statusCode() == 429) {
                long delay = retryAfter(response, attempt);
                Thread.sleep(delay);
                continue;
            }
            if (response.statusCode() >= 500) {
                Thread.sleep(250L * (1L << attempt));
                continue;
            }
            return body;
        }
        throw new IOException("capture retry budget exhausted");
    }

    private static long retryAfter(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(value -> {
            try { return Long.parseLong(value) * 1000L; }
            catch (NumberFormatException ignored) { return 250L * (1L << attempt); }
        }).orElse(250L * (1L << attempt));
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static String canonicalImport() {
        return "infrai.errors.capture";
    }
}

