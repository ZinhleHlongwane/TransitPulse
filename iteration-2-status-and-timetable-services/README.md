# Iteration 2: Status Service and Timetable Service

## Overview
Two additional services, each exposed over plain HTTP and talking to the Route-name Service for validation.

- **Status Service** — returns the current service status for a route or rank: on-time / delayed / cancelled for buses and trains, and loading / departing / full for minibus taxis (taxis don't run to a schedule the way buses and trains do).
- **Timetable Service** — returns a fixed timetable for buses/trains, and rank-based departure info for minibus taxis, scoped to route/rank + region, in JSON.

## What's implemented
- `util/SimpleJson.java` — a tiny dependency-free JSON builder/parser for this project's own flat payloads (no Jackson/Gson needed).
- `util/HttpUtil.java` — query-string parsing, JSON response writing, and a blocking HTTP GET helper built on `java.net.http.HttpClient`.
- `status/StatusService.java` — an HTTP server (`com.sun.net.httpserver.HttpServer`) exposing `GET /status?region=..&name=..`. Validates the route/rank against the Route-name Service (404 if unknown), then returns a mode-appropriate status. Includes a `forceStatus(...)` test hook used by later iterations' demos to simulate a status change.
- `status/StatusClient.java` — HTTP client for the Status Service. Translates network failures into `ServiceUnavailableException` and unparseable/missing-field responses into `MalformedResponseException`.
- `timetable/TimetableService.java` / `timetable/TimetableClient.java` — same pattern for timetables. For taxis, returns an empty time list plus a note explaining taxis depart once loaded.
- `errors/ServiceUnavailableException.java`, `errors/MalformedResponseException.java` — the two failure categories the clients raise; used more fully in Iteration 3.
- `Iteration2Demo.java` — starts both services, queries one bus, one train, and one taxi rank, and prints the mode-specific responses side by side.

## Run it

With Maven:

```bash
mvn compile exec:java
```

Or with plain javac/java (no Maven needed):

```bash
find src -name "*.java" > sources.txt
javac -d out @sources.txt
java -cp out transitpulse.Iteration2Demo
```

## Out of scope (for later iterations)
- Message Queue infrastructure, retries, and panic alerts (Iteration 3) — Iteration 2's demo shows an unknown-route query failing with a raw exception; that gets handled properly starting Iteration 3.
- Public-facing alerting (Iteration 4).
