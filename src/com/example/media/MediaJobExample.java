package com.example.media;

public final class MediaJobExample {
    private MediaJobExample() {}

    public static void main(String[] args) throws Exception {
        MediaJob job = new MediaJob("lesson-42-video", "course-physics", 3, 3, false);
        String decision = JobDecision.forJob(job);
        System.out.println(job.assetId() + " -> " + decision + " -> creator delivery paused");
        String key = System.getenv("INFRAI_API_KEY");
        if (key != null && !key.isBlank() && decision.equals("ALERT")) {
            String result = new InfraiErrorsClient(key).capture(job, "transcode attempts exhausted");
            System.out.println("capture envelope: " + result);
        }
    }
}

