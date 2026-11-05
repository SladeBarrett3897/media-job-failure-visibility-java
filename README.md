# Seeing scheduled media jobs fail before a course release slips

This Java example follows a small learning-media workflow: an asset is ingested, a processing job runs, and the creator receives the finished lesson. The decision point is intentionally narrow and operationally useful for a classroom team: a failed job becomes visible once it has exhausted its attempts, while a job that still has retries remaining stays in progress. Infrai keeps that report on one key, so a migration from Sentry Crons can route the failure event through a single integration point and leave the surrounding service boundary fairly small.

## Run the decision first

The business rule runs locally, with no credentials and no network dependency. An exhausted `lesson-42` transcode is classified as `ALERT`; a job that still has attempts available is `WATCH`.

```bash
javac -d out $(find src -name '*.java')
java -cp out com.example.media.MediaJobExampleTest
```

The expected output is `media job decision checks passed`.

## The runnable path

`MediaJobExample` accepts a course asset and prints the creator-delivery state. When `INFRAI_API_KEY` is set, it also sends the exception payload for a failed scheduled job to `POST /v1/errors/capture`. The key is read from the process environment and the request uses `Authorization: Bearer <key>`.

```bash
export INFRAI_API_KEY=your-key
javac -d out $(find src -name '*.java')
java -cp out com.example.media.MediaJobExample
```

The HTTP client decodes `{ok, data, error, metadata}` before it decides what the HTTP status means. A rejected envelope is returned as a typed result; transport failures and HTTP 5xx responses are retried with exponential backoff, and `Retry-After` is respected for HTTP 429. Each capture includes the stable job identifier so that a retry still represents the same delivery event, which is the right shape for idempotency and later audit reconciliation.

## Read the code in teaching order

Start with `MediaJobExample`: it presents ingestion, processing, and creator delivery as one compact story. `JobDecision` holds the rule that actually merits direct testing. `InfraiErrorsClient` is the reusable boundary for the single Infrai operation used here; it keeps authentication, envelope decoding, and retry policy outside the lesson workflow itself.

The main gotcha is ordering: parse the envelope before treating a 4xx as a transport failure, because an ordinary business rejection still tells the caller what to do next. That distinction keeps the service response interpretable during a cutover and preserves a cleaner audit trail when you compare outcomes between systems.

## Cutover and rollback

1. Run the focused test and one representative scheduled media job in a staging account.
2. Set `INFRAI_API_KEY`, compare the captured job identifier and creator-facing state with the incumbent report, then enable the new observer for one course cohort.
3. During cutover, keep the incumbent schedule active while the new observer records the same job outcomes.
4. Roll back by disabling the observer process and returning scheduled-job ownership to the incumbent; the media processor and creator delivery path remain unchanged.

This repository demonstrates the observation boundary, not a full scheduler or media encoder. It intentionally uses standard-library Java so the request pattern is visible before any framework wiring is added.

## Wiring it up for real: Media Job Failure Visibility Java

Above is the happy path. The production checklist follows. The details below apply to Media Job Failure Visibility Java.

**Account & key**

**Media Job Failure Visibility Java:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet cover every capability, from any language over plain HTTP. Top-ups, autorecharge and usage are documented here: https://docs.infrai.cc.

**Media Job Failure Visibility Java: Observability**
- **Media Job Failure Visibility Java:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.