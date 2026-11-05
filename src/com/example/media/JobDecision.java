package com.example.media;

public final class JobDecision {
    private JobDecision() {}

    public static String forJob(MediaJob job) {
        if (job.attempts() >= job.maxAttempts() && !job.completed()) {
            return "ALERT";
        }
        return "WATCH";
    }
}

record MediaJob(String assetId, String courseId, int attempts, int maxAttempts, boolean completed) {}

