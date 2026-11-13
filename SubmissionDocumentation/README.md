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

- JDK 17 or a compatible JDK supported by the installed Kotlin plugin
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
3. For a Railway-backed build, add `AURA_BACKEND_URL` and do not add provider
   keys to a release build.
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
in Railway, and run the worker as a separate service. The Railway deployment
uses the committed Prisma migration and runs `npm run db:migrate` before deploy.
Vercel is not used for this backend because the scheduled worker requires a
persistent process.

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

For a Railway-backed build, set `AURA_BACKEND_URL` in `local.properties`.
Release builds intentionally remove Gemini, Groq, and local Gmail client
secrets from `BuildConfig`; the app uses the authenticated backend AI service.

```bash
./gradlew assembleRelease
```

Do not ship Gemini/Groq keys inside an APK because values in `BuildConfig` can
be extracted from a released application. For evaluation without Google Play,
the debug APK is installable by sideloading:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

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

### Voice-to-action agent

- Accepts text commands and speech-to-text commands.
- Uses Gemini function calling with Groq fallback during local development.
- Uses the authenticated Railway AI proxy when `AURA_BACKEND_URL` is configured.
- Selects tools from a validated registry instead of executing arbitrary model output.
- Displays tool progress, confirmations, errors, and final results in the chat.
- Supports bounded multi-step actions with a five-tool-call limit per command.
- Provides optional Android Text-to-Speech responses with a mute button.

### Personal memory

- Persists memories in Room across app restarts.
- Stores user preferences and important information by key and category.
- Ranks relevant memories using query matches across key, value, and category.
- Retrieves recent conversation context for new commands.
- Provides memory viewing, editing, search, and deletion.
- Clears local conversation context when the user clears local data.

### Real-world actions

- Gmail OAuth connection, important-email listing, email reading, draft creation,
  and confirmation-gated sending.
- HTTPS-only browser automation with navigation, scrolling, extraction limits,
  redirect validation, and timeouts.
- Daily and weekly recurring tasks.
- Timers, alarms, and stopwatch tools.
- WorkManager background execution and boot recovery.
- Execution history and scheduled-task notifications.
- Backend authentication, AI proxying, Google OAuth token encryption, PostgreSQL,
  Prisma migrations, and a separate scheduled worker.

## 8. Incomplete features and known limitations

- Server-side Gmail read/send endpoints are not complete; the current Gmail
  action tools use Android-local Gmail OAuth.
- Android local memory and WorkManager tasks are device-local; they are not
  synchronized to the backend.
- WorkManager is not exact-time scheduling and may be deferred by Android.
- Server-side recurring workflows currently use a simple Railway worker loop;
  a production queue such as pg-boss would improve retries and observability.
- Vector memory search and multi-agent orchestration are not implemented.
- Device-specific performance measurements are still required.
- APK sideloading is supported; Google Play publishing configuration is outside
  this assignment scope.

## 9. Requirements coverage

| Assignment requirement | Implementation/status |
|---|---|
| Voice and text commands | Completed with speech recognition and Compose chat |
| AI natural-language planning and tool calling | Completed with Gemini/Groq and bounded orchestration |
| Persistent memory across restarts | Completed with Room |
| Relevant memory retrieval | Completed with relevance scoring and conversation context |
| View and delete memories | Completed in the Memory screen |
| Compose and send email | Completed locally; send requires explicit confirmation |
| Browse websites and extract information | Completed with controlled HTTPS WebView |
| Recurring scheduled tasks | Completed with Room, WorkManager, and boot recovery |
| Multi-step actions | Completed with ordered tool execution and a call limit |
| Modular Android architecture | Completed with UI, domain, data, tools, memory, and scheduler layers |
| Authentication and secure credentials | Backend JWT, encrypted Gmail tokens, encrypted local storage |
| Network and AI error handling | Explicit HTTP, provider, tool, worker, and repository errors |
| Android background constraints | WorkManager constraints, retries, notifications, and boot recovery |
| Backend production boundary | Implemented with Railway API, PostgreSQL, Prisma, and worker |
| Performance analysis | Procedure and verified build measurements documented; device metrics pending |
| Cost requirement | Uses free development tools and supports Gemini/Groq free-tier configuration |

## 10. Key technical challenges and solutions

### Reliable agent execution

**Challenge:** Model-generated actions can be invalid, repeated, or unsafe.

**Solution:** The orchestrator exposes only registered tools, validates every
argument, limits each command to five calls, records each result, and stops on
confirmation-required actions or execution failure.

### Persistent and relevant memory

**Challenge:** Recency-only memory retrieval becomes less useful as memories grow.

**Solution:** Room stores memories and conversation context persistently.
Retrieval scores matches across memory keys, values, and categories, with
stop-word filtering and exact-key bonuses.

### Android scheduling limits

**Challenge:** Android can stop or defer background work.

**Solution:** Tasks are persisted before scheduling, WorkManager uses unique
work names and network constraints, failed registration rolls back persistence,
and BootReceiver restores enabled tasks after restart. The UI reports execution
history instead of promising exact wall-clock execution.

### Secure production architecture

**Challenge:** Provider keys and Gmail refresh tokens must not be exposed in an APK.

**Solution:** Release builds blank provider keys, Android uses an authenticated
backend URL, Railway stores provider secrets in environment variables, and
server-side Gmail tokens are encrypted with AES-256-GCM.

### Controlled web access

**Challenge:** Website content is dynamic and untrusted.

**Solution:** BrowserTool accepts HTTPS URLs only, validates redirects, uses a
temporary JavaScript-enabled WebView, applies a timeout, limits extracted output,
and returns structured failures.

### Production database deployment

**Challenge:** The API and worker need the same schema without duplicate job claims.

**Solution:** Prisma owns the PostgreSQL schema and committed migration. Railway
runs migrations before deploy, while the worker claims jobs using database row
locking and records execution history.

## 11. Final submission checklist

- [x] Source code pushed to GitHub.
- [x] Railway API and worker configuration documented.
- [x] Initial Prisma migration committed.
- [x] APK build verified with the Railway backend URL configured locally.
- [x] APK output path: `app/build/outputs/apk/debug/app-debug.apk`.
- [x] Final APK SHA-256: `ed349f6a1ed945ea12149afa4b3b6cab747e60c0673e654d8305f652fbab7d58`.
- [x] APK installation command documented.
- [x] Secrets excluded from GitHub (`local.properties` and backend `.env`).
- [ ] Evaluator creates or receives a backend demo account.
- [ ] Evaluator installs and tests the APK on an Android device or emulator.
- [ ] Target-device performance measurements are collected.

## 12. Email submission package

Use the required subject format:

```text
Aurviz SDE Assignment Submission – [Candidate Name]
```

Include:

- GitHub repository URL.
- Railway backend URL and `/health` result.
- APK file or APK download link.
- Demo account credentials, if required by the evaluator.
- Link to [`SubmissionDocumentation/README.md`](./README.md).
- Link to [`ARCHITECTURE.md`](./ARCHITECTURE.md).
- Link to [`PERFORMANCE.md`](./PERFORMANCE.md).

Never include Railway environment values, API keys, OAuth client secrets, or
`local.properties` in the email.

## 13. Incomplete or production follow-up items

- Android now supports backend login/register and routes production AI calls
  through `/v1/ai/generate`. Gmail operations still use the local Android OAuth
  path; server-side Gmail API operation endpoints remain to be added.
- Backend Gmail and AI flows need an end-to-end Railway/PostgreSQL deployment test.
- A production release needs signing configuration, privacy policy, OAuth
  verification, and notification permission testing. These are not required
  for APK-only academic evaluation.
- Server-side scheduled email execution needs a production queue such as pg-boss
  and provider rate-limit handling.
- Device measurements must be collected on the target emulator or phone.

## 14. Architecture and design

See [ARCHITECTURE.md](./ARCHITECTURE.md).

## 15. Performance

See [PERFORMANCE.md](./PERFORMANCE.md).

## 16. Database

See [DATABASE.md](./DATABASE.md).
