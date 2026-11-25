# TransitPulse — final system

The consolidated TransitPulse implementation lives here. The original Iteration 1–4 coursework snapshots remain in their own directories and are not modified by this work.

## Current state

**Baseline compiles and automated tests pass.**

This is the separate Java 21 Maven project where the coursework implementation is being cleaned up in small steps. The original iteration folders remain available as historical snapshots.

## Run

Requirements: Java 21 and Maven.

```bash
mvn clean verify
mvn compile exec:java
```

The demo starts the in-memory Route-name and Status services, monitors a route, publishes status-change events through the in-memory message queue and sends console alerts through AlertBot.

## How the pieces fit together

- `routename` — sample route data and route normalisation/lookup.
- `model` — route records and transport modes.
- `status` — status HTTP service/client, resilience wrapper and change monitor.
- `timetable` — timetable service and client.
- `mq` — in-memory topic-based message delivery.
- `alertbot` — public-alert handling and console publisher.
- `errors` — service and malformed-response exceptions plus panic-alert publishing.
- `util` — HTTP and small JSON helpers.

## Message queue

The queue delivers messages asynchronously in publication order. Subscribers receive messages for topics they registered for. A failing subscriber is isolated from other subscribers, and subscriptions can be closed explicitly.

`awaitIdle(Duration)` lets the demo and tests wait for submitted deliveries to finish. `shutdown()` stops new publications while allowing submitted work to finish; `close()` performs a bounded graceful shutdown.

This is an in-memory demonstration queue. Messages are not persisted and will not survive a process restart. It is not a replacement for a production broker.

## Public-alert debounce

AlertBot uses a trailing-edge debounce per route:

1. A status-change event starts the quiet-period timer for that route.
2. If another event for the same route arrives before the timer expires, the pending alert is replaced and the timer restarts.
3. Once the route has had no further changes for the full debounce window, AlertBot publishes the latest status.
4. Different routes have independent timers.

This means a route that changes rapidly between states produces one alert for its final state after it settles, rather than an immediate alert followed by suppressed changes. Closing AlertBot cancels pending alerts and unsubscribes it from the queue.

The current publisher writes to the console. No external notification service is connected.

## Tests

JUnit 5 tests cover route cleaning and de-duplication, transport-mode parsing, basic JSON helper behaviour, retry exhaustion and interruption handling, message fan-out and ordering, subscriber failure isolation, unsubscribe, queue shutdown, idle waiting, panic-alert publication and AlertBot debounce behaviour.

Run the tests with:

```bash
mvn test
```

GitHub Actions runs `mvn clean verify` when this module changes.

## Known limitations and next work

- The message queue is in-memory and has no persistence or broker-level backpressure.
- Status and timetable demo data are held in memory.
- Public alerts are printed to the console; no external notification platform is connected.
- The JSON helper supports only the small flat payloads used by this project; it is not a general-purpose JSON parser.
- HTTP error handling and service lifecycle management still need further review.
- More integration tests are needed across the full route → status → message queue → AlertBot flow.

See the [architecture and cleanup plan](../docs/architecture.md) for the broader direction.
