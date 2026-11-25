# TransitPulse architecture and cleanup plan

This document describes the intended direction for the cleaned-up implementation. It is a plan, not a claim that the final system already exists.

## Preserve the coursework

The four `iteration-*` directories are historical snapshots. Keep their source files and individual Maven configurations intact while the final implementation is developed separately.

Do not make a cleanup commit that silently changes the behaviour or removes code from those snapshots.

## Target boundaries for final-system

The consolidated implementation should separate responsibilities rather than repeat the same packages in every iteration.

```text
final-system/
├── pom.xml
└── src/
    ├── main/java/transitpulse/
    │   ├── app/             # Application startup and wiring
    │   ├── domain/          # Routes, transport modes and status values
    │   ├── routes/          # Route cleaning and lookup
    │   ├── status/          # Status service and resilient client
    │   ├── timetable/       # Timetable service and client
    │   ├── http/            # HTTP server/client adapters
    │   ├── messaging/       # Event contracts and queue abstraction
    │   └── alerts/          # Panic alerts, monitoring and AlertBot
    └── test/java/transitpulse/
        ├── routes/
        ├── status/
        ├── messaging/
        └── alerts/
```

This is a proposed boundary map. Package names can change after reviewing dependencies and class responsibilities in detail.

## Integration flow

1. Route data is normalised and validated before downstream services use it.
2. The status and timetable capabilities expose their own service interfaces.
3. Clients communicate with services over HTTP and handle unavailable or malformed responses.
4. Resilience logic retries transient failures and reports exhausted failures through an event.
5. Status monitoring publishes a status-change event when a known route changes state.
6. AlertBot consumes status-change events and applies a debounce policy before handing the message to a public-alert publisher.

## Design constraints

- Keep the implementation in plain Java and the JDK initially. Do not add Spring Boot or a broker just to make the structure look more advanced.
- Keep HTTP transport concerns outside domain classes.
- Keep event payloads explicit and validate them at the boundary.
- Make retry count, backoff and debounce duration configurable rather than scattering constants.
- Give executors and HTTP servers explicit lifecycle management.
- Avoid swallowing interruption or hiding unexpected failures.
- Test behaviour at the boundaries: malformed route rows, HTTP errors, service timeouts, retry exhaustion, event delivery and debounce windows.
- Preserve the current console publisher as a local demo adapter until a real provider is deliberately selected.

## Implementation sequence

1. Map existing Iteration 4 classes and their dependencies.
2. Create the final-system build and move/copy only the required implementation into its new package boundaries.
3. Compile the new module before changing behaviour.
4. Add tests around the existing behaviour.
5. Refactor one responsibility at a time, keeping the iteration snapshots untouched.
6. Update the root README only when the final-system implementation and its verification status are real.

## Current state

The four coursework snapshots exist and remain independently runnable through the root `run.sh` script. The final-system directory is a planning scaffold at this stage; no consolidated implementation or automated test suite has been added there yet.
