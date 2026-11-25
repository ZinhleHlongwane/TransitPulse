# TransitPulse — final system

The consolidated TransitPulse implementation lives here. The original Iteration 1–4 coursework snapshots remain in their own directories and are not modified by this work.

## Current state

**Baseline copied; first build verification pending.**

The current source is a clean starting copy of the Iteration 4 implementation. It gives the final system one Maven project and one source tree, separate from the historical snapshots. It is not yet the finished refactor: some original design issues are still present and will be addressed in small, testable steps.

## Run

Requirements: Java 21 and Maven.

```bash
mvn clean verify
mvn compile exec:java
```

The demo starts the in-memory Route-name and Status services, monitors a route, publishes status-change events through the in-memory message queue and sends console alerts through AlertBot.

## Current package responsibilities

- `routename` — sample route data and route normalisation/lookup.
- `model` — route records and transport modes.
- `status` — status HTTP service/client, resilience wrapper and change monitor.
- `timetable` — timetable service and client.
- `mq` — in-memory topic-based message delivery.
- `alertbot` — public-alert handling and console publisher.
- `errors` — service and malformed-response exceptions plus panic-alert publishing.
- `util` — HTTP and small JSON helpers.

These are the initial package boundaries carried over from Iteration 4. They will be reviewed and improved as the implementation is tested.

## Known issues to address

- Replace timing-based sleeps in the demo with deterministic coordination and queue shutdown/await behaviour.
- Make retry interruption stop the retry loop immediately.
- Decide and implement an explicit alert cooldown/debounce policy.
- Distinguish HTTP client errors from server errors and connection failures.
- Replace the hand-written JSON helper with a safer parser or a deliberately constrained, tested format.
- Add unit and integration tests before changing behaviour.

See the [architecture and cleanup plan](../docs/architecture.md) for the intended sequence.