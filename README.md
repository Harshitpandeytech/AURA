# AURA Personal Agent

A native Android personal assistant foundation built with Kotlin, Jetpack Compose, Room, WorkManager, Gemini, Gmail OAuth, and controlled WebView browsing.

## Setup

1. Use JDK 17 and Android SDK API 34.
2. Copy `local.properties.example` to `local.properties`.
3. Set `sdk.dir`, `GEMINI_API_KEY`, and `GMAIL_CLIENT_ID`.
4. Enable Gmail API and configure OAuth consent in Google Cloud.
5. Register `com.aurviz.personalagent:/oauth2redirect` as the OAuth redirect URI.
6. Build with:

```bash
./gradlew clean testDebugUnitTest
./gradlew assembleDebug
```

## Implemented

- Room-backed personal memory CRUD and keyword search.
- Gemini function calling through one bounded agent orchestrator.
- Text and speech-to-text chat input.
- Gmail OAuth, important-email retrieval, reading, draft creation, and confirmation-gated sending.
- HTTPS-only WebView content extraction and scrolling.
- Daily/weekly natural-language scheduling with persisted tasks, WorkManager execution, and execution history.
- Agent-created recurring schedules through the validated `schedule_task` tool.
- Room-backed pending-action records for confirmation-gated email sending.
- Boot recovery that re-registers enabled recurring tasks after device restart.

## Deliberate limitations

WorkManager is inexact and may defer execution. Voice output, vector search, multi-agent orchestration, exact alarms, and unsupported integrations are not included.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/DATABASE.md](docs/DATABASE.md), and [docs/PERFORMANCE.md](docs/PERFORMANCE.md).
