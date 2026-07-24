# Iteration 4: Public Alerts

## Overview
An **AlertBot** that proactively notifies the public when a route's or rank's status changes, closing the loop from internal MQ events to external user-facing communication.

## What's implemented
- `status/StatusChangeMonitor.java` — watches a route/rank via the resilient Status Service client and publishes a `status-change` event to the MQ whenever the observed status differs from what was last seen. It doesn't know or care who's listening — that's the AlertBot's job.
- `alertbot/AlertBot.java` — subscribes to the `status-change` topic and turns events into public alerts, **debounced** per route/rank so a status that flaps within a short window (default: configurable `Duration`) produces one alert, not several.
- `alertbot/PublicAlertPublisher.java` — the pluggable posting interface. `alertbot/ConsolePublicAlertPublisher.java` is the demo implementation (prints to console instead of actually posting to Twitter/X); swap in a real API client behind the same interface without touching `AlertBot`'s logic.
- `Iteration4Demo.java` — wires the full pipeline (Route-name Service → Status Service → MQ → AlertBot → public feed) and demonstrates three scenarios in sequence:
  1. A real status change (on-time → delayed) → **one public alert.**
  2. A rapid flap (on-time → delayed → on-time) within the debounce window → **suppressed, no alert.**
  3. A change after the debounce window has passed → **one more public alert.**

## Run it

With Maven:

```bash
mvn compile exec:java
```

Or with plain javac/java (no Maven needed):

```bash
find src -name "*.java" > sources.txt
javac -d out @sources.txt
java -cp out transitpulse.Iteration4Demo
```

## Wrap-up
By this point the full distributed pipeline is exercised end-to-end: Route-name Service → Status/Timetable Services → MQ-based error handling and panic alerts → AlertBot public notifications — for buses, trains, and minibus taxis alike.
