# TransitPulse

## Systems Integration

Students build **TransitPulse**, a distributed public-transport status information system covering buses, trains, and minibus taxis, with a strong emphasis on how multiple independent services communicate across networks. Core themes are fault tolerance, partial failure handling, data consistency, and proactive monitoring. Students gain practical skills in Message Queue infrastructure and distributed system design throughout the four iterations.

### Iteration 1: Route-name Service

Students create the foundational Route-name service, cleaning data containing bus, train, and minibus taxi route/rank names across regions. This lays the groundwork for data handling and processing within a distributed system.

### Iteration 2: Status Service and Timetable Service

Two additional services: the Status service provides current delay/service status upon query — including bus and train delays and minibus taxi rank loading/departure status; the Timetable service delivers fixed timetables for buses and trains, and rank-based departure info for minibus taxis, for specific routes and regions in JSON format. Focus on inter-service communication.

### Iteration 3: Error Handling and Panic Alerts

Students enhance system robustness by implementing error handling and panic alerts, integrating Message Queue infrastructure to improve performance and resilience. Focus on fault tolerance and proactive monitoring.

### Iteration 4: Public Alerts

An AlertBot is implemented to proactively notify users of service-status changes via public-facing social media platforms, reinforcing responsiveness and user interaction in distributed systems.

---

## This project includes working solutions and some in progress

Every iteration folder below is a **complete, independently-compilable snapshot** of the system as it stands at that milestone (iteration 2's folder includes iteration 1's code too, and so on) — the same way a real cumulative project submission works. Each README explains what that iteration adds and how to run its demo.

**No external dependencies.** Everything is written against the plain JDK — `com.sun.net.httpserver.HttpServer` and `java.net.http.HttpClient` for networking, and a small hand-rolled JSON helper instead of Jackson/Gson. That means it builds either way below, and needs no libraries beyond the two small Maven plugins used to run it. Requires **Java 17+** (built and tested on Java 21).

## Repository layout

```
TransitPulse/
├── README.md
├── iteration-1-route-name-service/    (pom.xml + src/main/java, standard Maven layout)
├── iteration-2-status-and-timetable-services/
├── iteration-3-error-handling-and-panic-alerts/
└── iteration-4-public-alerts/
```

Each iteration folder is already a self-contained Maven project (`src/main/java/...` is the standard Maven source layout) — just `cd` into whichever iteration you want and run Maven from there.

## Quick start — one command

```bash
./run.sh
```

Runs the final, full system (Iteration 4) end to end. Uses Maven if it's on your `PATH`, and automatically falls back to plain `javac`/`java` if it isn't — so this works either way, no setup decisions needed.

To run an earlier iteration instead:

```bash
./run.sh 1   # Route-name Service only
./run.sh 2   # + Status and Timetable Services
./run.sh 3   # + error handling, MQ, panic alerts
./run.sh 4   # full system (default)
```

If `run.sh` won't execute directly (e.g. `Permission denied`), run it as `bash run.sh` instead, or `chmod +x run.sh` once.

## Quick start — Maven (run the final, full system)

```bash
cd iteration-4-public-alerts
mvn compile exec:java
```

That runs the `exec.mainClass` set in that folder's `pom.xml` (`Iteration4Demo` for this folder). To run a different demo class from the same folder without editing the pom:

```bash
mvn compile exec:java -Dexec.mainClass=transitpulse.Iteration1Demo
```

`cd` into any of the other iteration folders and run the same `mvn compile exec:java` command to see that milestone on its own.

## Quick start — plain javac/java (no Maven needed)

```bash
cd iteration-4-public-alerts
find src -name "*.java" > sources.txt
javac -d out @sources.txt
java -cp out transitpulse.Iteration4Demo
```

Swap `Iteration4Demo` for `Iteration1Demo` / `Iteration2Demo` / `Iteration3Demo` to see earlier iterations run on their own, or `cd` into any iteration folder and use the same commands there.

## What each demo shows

| Demo | What it demonstrates |
|---|---|
| `Iteration1Demo` | Loads messy raw route/rank data, cleans and de-duplicates it, looks up an entry. |
| `Iteration2Demo` | Starts the Status + Timetable services over HTTP, queries a bus, a train, and a taxi rank, shows mode-specific responses. |
| `Iteration3Demo` | Same as above, but then kills the Status Service mid-run to show retry-with-backoff, graceful degradation, and a panic alert published to the MQ. |
| `Iteration4Demo` | Full pipeline: a real status change triggers one public alert; a rapid flap between statuses is debounced into silence; a change after the debounce window produces a second alert. |

## Known simplifications (worth knowing, not bugs)

- The Message Queue is an in-memory pub/sub class (`transitpulse.mq.MessageQueue`), not a real broker like RabbitMQ/ActiveMQ — swapped in to keep the project dependency-free. The publish/subscribe API is written the way it would be against a real broker, so replacing it later is a matter of swapping the implementation, not the calling code.
- `RouteNameService`'s title-casing is naive (e.g. "KwaZulu-Natal" becomes "Kwazulu-natal") — a good candidate for you to improve as an exercise.
- Data is in-memory only (no database) — restarting a demo starts fresh.
