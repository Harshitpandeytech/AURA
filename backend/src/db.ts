import { PrismaClient } from "@prisma/client";
import { Pool, type PoolClient } from "pg";
import { config } from "./config.js";

export const prisma = new PrismaClient();

export const pool = new Pool({
  connectionString: config.DATABASE_URL,
  ssl: config.DATABASE_URL.includes("localhost") ? false : { rejectUnauthorized: false }
});

export async function withTransaction<T>(work: (client: PoolClient) => Promise<T>): Promise<T> {
  const client = await pool.connect();
  try {
    await client.query("BEGIN");
    const result = await work(client);
    await client.query("COMMIT");
    return result;
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
  }
}
