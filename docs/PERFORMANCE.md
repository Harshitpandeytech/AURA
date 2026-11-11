# Performance

Performance measurements must be collected on the target emulator/device after configuring JDK 17, Android SDK, Gemini, and Gmail OAuth.

| Measurement | Result | Method |
|---|---:|---|
| Agent response time | Pending device run | Measure from send action to final assistant message |
| Scheduled task latency | Pending device run | Compare scheduled `nextRunAt` with execution-log timestamp |
| Memory usage | Pending device run | Android Studio Profiler during chat, browsing, and worker execution |

WorkManager timing is intentionally reported as latency rather than exact-time compliance because Android can defer background work.
