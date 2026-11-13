import express from "express";
import cors from "cors";
import helmet from "helmet";
import { z } from "zod";
import { config } from "./config.js";
import { pool, prisma } from "./db.js";
import { generateWithProvider } from "./ai.js";
import { credentialsSchema, login, register, requireAuth, type AuthenticatedRequest } from "./auth.js";
import { completeGoogleAuthorization, googleAuthorizationUrl } from "./gmail.js";

const app = express();
app.use(helmet());
app.use(cors({
  origin: config.CORS_ORIGIN
    ? config.CORS_ORIGIN.split(",").map((value) => value.trim())
    : false
}));
app.use(express.json({ limit: "256kb" }));

app.get("/health", async (_request, response) => {
  try {
    await prisma.$queryRaw`SELECT 1`;
    response.json({ status: "ok" });
  } catch {
    response.status(503).json({ status: "unavailable" });
  }
});

app.post("/v1/auth/register", async (request, response) => {
  try {
    const input = credentialsSchema.parse(request.body);
    response.status(201).json(await register(input.email, input.password));
  } catch (error) {
    response.status(400).json({ error: error instanceof Error ? error.message : "Registration failed" });
  }
});

app.post("/v1/auth/login", async (request, response) => {
  try {
    const input = credentialsSchema.parse(request.body);
    response.json(await login(input.email, input.password));
  } catch {
    response.status(401).json({ error: "Invalid email or password" });
  }
});

app.post("/v1/ai/generate", requireAuth, async (request: AuthenticatedRequest, response) => {
  try {
    const input = z.object({ contents: z.array(z.unknown()), tools: z.array(z.unknown()).default([]) }).parse(request.body);
    response.json(await generateWithProvider(input));
  } catch (error) {
    response.status(502).json({ error: error instanceof Error ? error.message : "AI provider failed" });
  }
});

app.post("/v1/jobs", requireAuth, async (request: AuthenticatedRequest, response) => {
  try {
    const input = z.object({
      instruction: z.string().trim().min(1).max(10_000),
      nextRunAt: z.coerce.date(),
      intervalSeconds: z.number().int().min(900).max(31_536_000).default(86_400)
    }).parse(request.body);
    const result = await pool.query(
      `INSERT INTO scheduled_jobs (user_id, instruction, next_run_at, interval_seconds)
       VALUES ($1, $2, $3, $4)
       RETURNING id, instruction, next_run_at, interval_seconds, enabled`,
      [request.userId, input.instruction, input.nextRunAt, input.intervalSeconds]
    );
    response.status(201).json(result.rows[0]);
  } catch (error) {
    response.status(400).json({ error: error instanceof Error ? error.message : "Unable to create job" });
  }
});

app.get("/v1/jobs", requireAuth, async (request: AuthenticatedRequest, response) => {
  const result = await pool.query(
    `SELECT id, instruction, next_run_at, interval_seconds, enabled
     FROM scheduled_jobs WHERE user_id = $1 ORDER BY next_run_at`,
    [request.userId]
  );
  response.json(result.rows);
});

app.get("/v1/auth/google/start", requireAuth, async (request: AuthenticatedRequest, response) => {
  try {
    response.redirect(await googleAuthorizationUrl(request.userId!));
  } catch (error) {
    response.status(503).json({ error: error instanceof Error ? error.message : "Google OAuth unavailable" });
  }
});

app.get("/v1/auth/google/callback", async (request, response) => {
  try {
    const code = z.string().min(1).parse(request.query.code);
    const state = z.string().min(1).parse(request.query.state);
    await completeGoogleAuthorization(code, state);
    response.redirect(config.ANDROID_APP_REDIRECT_URI);
  } catch (error) {
    response.status(400).json({ error: error instanceof Error ? error.message : "Google OAuth failed" });
  }
});

app.listen(config.PORT, () => {
  console.log(`AURA backend listening on port ${config.PORT}`);
});
