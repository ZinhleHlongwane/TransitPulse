# Iteration 3: Error Handling and Panic Alerts

## Overview
Hardens the system built in Iterations 1 and 2. Introduces a Message Queue (MQ) so services can communicate asynchronously and panic alerts don't depend on anyone actively watching at the moment something breaks.

## What's implemented
- `mq/MessageQueue.java` — a minimal in-memory topic-based publish/subscribe queue, standing in for a real broker (RabbitMQ/ActiveMQ) so the whole project stays dependency-free. Delivery to subscribers happens asynchronously via a thread pool, so a slow/failing subscriber never blocks the publisher — the core resilience benefit of using an MQ at all. `mq/Message.java` is the message envelope (topic, payload, timestamp).
- `errors/PanicAlertPublisher.java` — publishes structured JSON alerts to the `panic-alerts` topic when a dependency is unreachable or returns malformed data.
- `status/ResilientStatusClient.java` — wraps `StatusClient` with retry-with-backoff (3 attempts) against `ServiceUnavailableException`, and gives up immediately (no point retrying) on `MalformedResponseException`. Either way, callers get an `Optional<StatusInfo>` back instead of an exception — the calling code decides how to degrade (skip, use a cached value, etc.) — and a panic alert is published on the way out.
- `Iteration3Demo.java` — queries the Status Service while it's healthy, then **deliberately stops the service mid-run** to simulate a real outage, and shows: the retry attempts happening, the caller degrading gracefully instead of crashing, and a panic alert arriving on the MQ.

## Run it

With Maven:

```bash
mvn compile exec:java
```

Or with plain javac/java (no Maven needed):

```bash
find src -name "*.java" > sources.txt
javac -d out @sources.txt
java -cp out transitpulse.Iteration3Demo
```

You should see a successful query, then the simulated outage, then a `>>> [panic-alerts subscriber] received: ...` line showing the alert was published and consumed.

## Out of scope (for later iterations)
- Public/social-media-facing notifications and debouncing (Iteration 4).
