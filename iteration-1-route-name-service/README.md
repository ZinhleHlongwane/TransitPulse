# Iteration 1: Route-name Service

## Overview
The foundational **Route-name Service**. This service is responsible for ingesting and cleaning raw route/rank data (bus routes, train routes, and minibus taxi ranks and routes across regions) so that later services can rely on consistent, well-formed identifiers.

## What's implemented
- `model/TransportMode.java` — enum for BUS / TRAIN / TAXI, with a lenient parser that also accepts "minibus" and "minibus taxi" as raw input.
- `model/RouteEntry.java` — the cleaned record: region, name, code, mode. Defines an `identityKey()` (region + name, case-insensitive) used for de-duplication and lookups.
- `routename/RawRouteData.java` — a deliberately messy sample dataset: inconsistent casing, stray whitespace, and duplicate rows recorded with different formatting, to exercise the cleaning logic.
- `routename/RouteNameService.java` — the cleaning pipeline:
  - trims whitespace on every field
  - normalizes region/name to Title Case and code to UPPERCASE
  - parses mode leniently
  - skips (rather than crashes on) malformed rows
  - de-duplicates same region+name+mode combinations that were recorded with different formatting
  - exposes `all()`, `findByRegion(region)`, `find(region, name)`, `exists(region, name)`
- `Iteration1Demo.java` — loads the messy sample data, prints the cleaned/de-duplicated result, and demonstrates a lookup.

## Run it

With Maven:

```bash
mvn compile exec:java
```

Or with plain javac/java (no Maven needed):

```bash
find src -name "*.java" > sources.txt
javac -d out @sources.txt
java -cp out transitpulse.Iteration1Demo
```

## Known simplification
Title-casing is naive — it capitalizes each whitespace-separated word, so hyphenated names like "KwaZulu-Natal" come out as "Kwazulu-natal". A good next exercise: improve `titleCase()` to handle hyphens and known acronyms properly.

## Out of scope (for later iterations)
- Real-time status or delay information (Iteration 2).
- Network/socket-based communication between services (Iteration 2+).
