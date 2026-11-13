import { pool } from "./db.js";
import { generateWithProvider } from "./ai.js";

const POLL_INTERVAL_MS = 30_000;

async function runDueJobs() {
  const client = await pool.connect();
  try {
    await client.query("BEGIN");
    const jobs = await client.query(
      `SELECT id, user_id, instruction, interval_seconds
       FROM scheduled_jobs
       WHERE enabled = TRUE AND next_run_at <= NOW()
       ORDER BY next_run_at
       FOR UPDATE SKIP LOCKED
       LIMIT 20`
    );
    for (const job of jobs.rows) {
      await client.query(
        "UPDATE scheduled_jobs SET next_run_at = next_run_at + ($2 * interval '1 second'), updated_at = NOW() WHERE id = $1",
        [job.id, job.interval_seconds]
      );
    }
    await client.query("COMMIT");
    for (const job of jobs.rows) {
      const startedAt = new Date();
      try {
        const result = await generateWithProvider({
          contents: [{ role: "user", parts: [{ text: job.instruction }] }]
        });
        await pool.query(
          `INSERT INTO scheduled_job_runs (job_id, status, result, started_at, completed_at)
           VALUES ($1, 'success', $2, $3, NOW())`,
          [job.id, JSON.stringify(result), startedAt]
        );
      } catch (error) {
        await pool.query(
          `INSERT INTO scheduled_job_runs (job_id, status, error, started_at, completed_at)
           VALUES ($1, 'failure', $2, $3, NOW())`,
          [job.id, error instanceof Error ? error.message : "Scheduled job failed", startedAt]
        );
      }
    }
  } catch (error) {
    await client.query("ROLLBACK");
    console.error("Scheduled job polling failed", error);
  } finally {
    client.release();
  }
}

setInterval(() => void runDueJobs(), POLL_INTERVAL_MS);
void runDueJobs();
