# AURA Personal Agent

A native Android personal assistant foundation built with Kotlin, Jetpack Compose, Room, WorkManager, Gemini, Gmail OAuth, and controlled WebView browsing.

The repository also contains a separate production backend in [`backend/`](backend/).
The backend is a Node.js/TypeScript service intended for Railway with PostgreSQL.

## Setup

1. Use JDK 17 and Android SDK API 34.
2. Copy `local.properties.example` to `local.properties`.
3. Use JDK 17, then set `sdk.dir`, `GEMINI_API_KEY`, and `GMAIL_CLIENT_ID`.
4. Enable the Gmail API and configure the OAuth consent screen in Google Cloud.
5. Create an Android OAuth client for this app. The redirect scheme is derived
   from the client ID, so no manually registered redirect URI is needed.
6. Build with:

```bash
./gradlew clean testDebugUnitTest
./gradlew assembleDebug
```

## Production backend

The Android app is suitable for local development with provider keys in ignored
`local.properties`. For publishing, deploy [`backend/`](backend/) and move Gemini,
Groq, Google OAuth, and token-encryption secrets into Railway environment variables.
The backend provides multi-user registration/login, authenticated AI proxying, server-side
Google OAuth, encrypted Gmail token storage, health checks, and a separate scheduled-job
worker. Do not ship provider API keys in the APK.

See [backend/README.md](backend/README.md) for Railway setup and the required PostgreSQL
Prisma schema and migrations. Set `AURA_BACKEND_URL` in `local.properties` for a production build. Release
builds route AI through the authenticated backend and do not package Gemini or
Groq keys. The direct-provider path remains for local debug development.

## Implemented

- Room-backed personal memory CRUD and keyword search.
- Gemini function calling through one bounded agent orchestrator.
- Text and speech-to-text chat input.
- Gmail OAuth, important-email retrieval, reading, draft creation, and confirmation-gated sending.
- Gmail must be connected from Settings before email tools can access the account.
- HTTPS-only WebView content extraction and scrolling for public websites (15-second timeout and 12,000-character output cap).
- Daily/weekly natural-language scheduling with persisted tasks, WorkManager execution, and execution history.
- Agent-created recurring schedules through the validated `schedule_task` tool.
- Bounded multi-step workflows that execute tools in sequence and report results in chat or notifications.
- Room-backed pending-action records for confirmation-gated email sending.
- Boot recovery that re-registers enabled recurring tasks after device restart.

## Deliberate limitations

WorkManager is inexact and may defer execution. Vector search and multi-agent
orchestration are not included. Response speech uses the device's Android Text-to-Speech
engine and can be turned off from the speaker button in Chat. Android may open the exact-alarm
permission screen when an alarm is created; notification permission must also be
allowed for alarm notifications to appear.

See [SubmissionDocumentation/README.md](SubmissionDocumentation/README.md) for the complete setup, architecture, performance, database, and submission report.

## Final APK

The final debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Build it with the Railway backend URL configured in ignored `local.properties`:

```properties
AURA_BACKEND_URL=https://aura-production-144f5f70.up.railway.app
```

```bash
./gradlew assembleDebug
```

Install on a connected Android device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The verified APK checksum from the final build is:

```text
ed349f6a1ed945ea12149afa4b3b6cab747e60c0673e654d8305f652fbab7d58
```
