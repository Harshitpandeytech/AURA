# Performance Analysis

Measurements must be collected on the target emulator/device after configuring
JDK 17, Android SDK, Gemini, and Gmail OAuth. No device or production database
was available in this workspace, so device-dependent values are intentionally
marked as pending rather than invented.

| Measurement | Result | Method |
|---|---:|---|
| Agent response time | Pending target-device run | Measure from send action to final assistant message |
| Scheduled task latency | Pending target-device run | Compare scheduled `nextRunAt` with execution-log timestamp |
| Memory usage | Pending target-device run | Android Studio Profiler during chat, browsing, and worker execution |
| Backend build time | Verified locally | TypeScript production build completed successfully |
| Backend dependency audit | Action required | Prisma CLI currently reports a development-time advisory |

## Measurement procedure

1. Run the app on the target emulator or phone.
2. Send five normal commands and record the time from send to final response.
3. Create a recurring task and compare its requested time with the execution log.
4. Run Android Studio Profiler during chat, browsing, and a scheduled task.
5. Report median and maximum values in the table above.

## Optimization already used

The agent limits each request to five tool calls, limits memory context, caps
browser output at 12,000 characters, and uses bounded Room queries. This avoids
unbounded prompts and prevents a failed reasoning loop from consuming resources.

## Further optimization opportunity

Use a server-side queue and provider response streaming in the production
backend. This would reduce time-to-first-response and isolate Android from
provider retries while preserving the same bounded tool loop.

WorkManager timing is intentionally reported as latency rather than exact-time
compliance because Android can defer background work.
