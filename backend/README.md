# AURA backend

This service is the production boundary for multi-user authentication, AI provider
secrets, Gmail OAuth tokens, and server-side scheduled jobs. It is designed to run
as a Node 22 service with PostgreSQL on Railway.

## Local setup

```bash
npm install
cp .env.example .env
npm run build
node dist/server.js
```

Create the PostgreSQL schema before starting:

```bash
npm run db:generate
npx prisma migrate dev --name init
```

Generate secrets instead of copying the example values:

```bash
openssl rand -base64 32
openssl rand -base64 32
```

The Android client should call `/v1/auth/login` or `/v1/auth/register`, then send
the returned bearer token to `/v1/ai/generate`. AI keys never belong in the APK.
Gmail OAuth starts at `/v1/auth/google/start` and stores tokens encrypted at rest.

The worker is a separate Railway process and executes due AI workflows:

```bash
npm run worker
```

Use a persistent Railway PostgreSQL database and run the worker as a separate
service. `FOR UPDATE SKIP LOCKED` prevents duplicate claims when multiple worker
instances are running. The `/v1/jobs` endpoint creates and lists authenticated
per-user jobs; job runs are recorded in PostgreSQL.

Prisma is the typed schema and migration source in [`prisma/schema.prisma`](prisma/schema.prisma).
The repository includes the initial migration under `prisma/migrations/`. Railway
runs `npm run db:migrate` as a pre-deploy step before starting the API and worker
services. The legacy
`src/schema.sql` remains available for inspecting the original tables during migration.
