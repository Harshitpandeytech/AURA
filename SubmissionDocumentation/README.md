# AURA Submission Documentation

## 1. Project summary

AURA is a native Android personal assistant. It accepts text or voice commands,
uses Gemini/Groq function calling, stores personal memory locally, controls
approved tools, and runs recurring tasks with WorkManager.

The repository also contains a separate Node.js/TypeScript backend in
[`../backend/`](../backend/) for production authentication, AI proxying, Gmail
OAuth, encrypted token storage, PostgreSQL, and server-side jobs.

## 2. Requirements

### Android

- JDK 17
- Android SDK API 34
- Android Studio or the Android SDK command-line tools
- A connected Android device or emulator
- A configured Android Text-to-Speech engine for spoken responses

### Backend

- Node.js 22.18 or newer
- npm
- PostgreSQL
- Railway account for deployment

## 3. Android setup

1. Copy the sample configuration:

   ```bash
   cp local.properties.example local.properties
   ```

2. Set `sdk.dir` in `local.properties`.
3. For local development only, add a Gemini key and Gmail OAuth client ID.
4. Enable the Gmail API and configure the OAuth consent screen in Google Cloud.
5. Use an Android OAuth client. The app derives its redirect scheme from the
   client ID.

Never commit `local.properties`. It is ignored by Git.

## 4. Backend setup

1. Enter the backend directory:

   ```bash
   cd backend
   npm install
   cp .env.example .env
   ```

2. Set these environment variables without committing `.env`:

   ```text
   DATABASE_URL=postgresql://...
   JWT_SECRET=<long random secret>
   TOKEN_ENCRYPTION_KEY=<base64 encoded 32-byte key>
   GEMINI_API_KEY=<server-side key>
   GOOGLE_CLIENT_ID=<server-side OAuth client>
   GOOGLE_CLIENT_SECRET=<server-side OAuth secret>
   GOOGLE_REDIRECT_URI=https://your-backend-domain/v1/auth/google/callback
   ```

3. Generate the Prisma client and create migrations:

   ```bash
   npm run db:generate
   npx prisma migrate dev --name init
   ```

4. Build and start the API:

   ```bash
   npm run build
   npm start
   ```

5. Start the scheduled-job worker separately:

   ```bash
   npm run worker
   ```

For Railway, use a persistent PostgreSQL service, deploy the backend using
[`../backend/Dockerfile`](../backend/Dockerfile), add the environment variables
in Railway, and run the worker as a separate service.

## 5. Build and run the APK

Run unit tests:

```bash
./gradlew testDebugUnitTest
```

Build a debug APK:

```bash
./gradlew assembleDebug
```

The APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For a release build:

```bash
./gradlew assembleRelease
```

The release build must use the backend for AI and Gmail operations. Do not
ship Gemini/Groq keys inside an APK because values in `BuildConfig` can be
extracted from a released application.

## 6. API smoke checks

After starting the backend:

```bash
curl http://localhost:8080/health
```

Expected response:

```json
{"status":"ok"}
```

Register a test user:

```bash
curl -X POST http://localhost:8080/v1/auth/register \
  -H 'content-type: application/json' \
  -d '{"email":"test@example.com","password":"a-long-test-password"}'
```

Use the returned bearer token for `/v1/ai/generate`, `/v1/jobs`, and Gmail OAuth
start requests.

## 7. Completed features

- Native Kotlin Android application with Jetpack Compose.
- Text and speech-to-text input.
- Android Text-to-Speech responses with a mute button.
- Persistent Room-backed personal memory.
- Memory search, editing, and deletion.
- Recent conversation context persisted locally.
- Gemini function calling with Groq fallback.
- Bounded multi-step tool execution.
- Gmail OAuth, email reading, draft preparation, and confirmation-gated sending.
- HTTPS-only WebView browsing with scrolling and text extraction.
- Daily and weekly recurring tasks.
- WorkManager background execution and boot recovery.
- Execution history and scheduled-task notifications.
- Backend scaffold with JWT authentication, Prisma schema, AI proxy, Gmail OAuth,
  encrypted token storage, and PostgreSQL scheduled jobs.

## 8. Incomplete or production follow-up items

- The Android production flavor still needs to be connected to the backend login
  and `/v1` API instead of direct provider calls.
- Backend Gmail and AI flows need an end-to-end Railway/PostgreSQL deployment test.
- A production release needs signing configuration, Play App Signing, privacy
  policy, OAuth verification, and notification permission testing.
- Server-side scheduled email execution needs a production queue such as pg-boss
  and provider rate-limit handling.
- Device measurements must be collected on the target emulator or phone.

## 9. Architecture and design

See [ARCHITECTURE.md](./ARCHITECTURE.md).

## 10. Performance

See [PERFORMANCE.md](./PERFORMANCE.md).

## 11. Database

See [DATABASE.md](./DATABASE.md).

## 12. Main engineering challenges

### Safe tool execution

The model can request tools, but the orchestrator validates tool names and
arguments, limits calls per request, records execution results, and requires
confirmation for email sending.

### Android background limits

WorkManager is used instead of a continuously running service. This supports
network constraints and boot recovery, but Android may defer execution.

### Credential safety

Local Gmail tokens use encrypted Android storage. The production backend encrypts
server-side Gmail tokens and keeps AI provider keys outside the APK.

### Failure handling

Network, AI, Gmail, tool, worker, and repository failures are surfaced to the
user or execution history. Scheduled failures are logged and retried by
WorkManager where appropriate.
