package com.example.media;

public final class MediaJobExampleTest {
    public static void main(String[] args) {
        MediaJob exhausted = new MediaJob("lesson-42", "course-physics", 3, 3, false);
        MediaJob retryable = new MediaJob("lesson-43", "course-physics", 2, 3, false);
        MediaJob finished = new MediaJob("lesson-44", "course-physics", 3, 3, true);
        require("ALERT".equals(JobDecision.forJob(exhausted)), "exhausted work must alert");
        require("WATCH".equals(JobDecision.forJob(retryable)), "retryable work must stay visible");
        require("WATCH".equals(JobDecision.forJob(finished)), "finished work must not alert");
        System.out.println("media job decision checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

