import { randomUUID } from "node:crypto";
import type { Request, Response, NextFunction } from "express";
import { z } from "zod";
import { pool } from "./db.js";
import { createAccessToken, hashPassword, readAccessToken, verifyPassword } from "./security.js";

export const credentialsSchema = z.object({
  email: z.string().email().max(320),
  password: z.string().min(12).max(200)
});

export async function register(email: string, password: string) {
  const normalizedEmail = email.trim().toLowerCase();
  const passwordHash = await hashPassword(password);
  const result = await pool.query(
    "INSERT INTO users (id, email, password_hash) VALUES ($1, $2, $3) RETURNING id, email",
    [randomUUID(), normalizedEmail, passwordHash]
  );
  const user = result.rows[0];
  return { user, accessToken: await createAccessToken(user.id) };
}

export async function login(email: string, password: string) {
  const result = await pool.query("SELECT id, email, password_hash FROM users WHERE email = $1", [
    email.trim().toLowerCase()
  ]);
  const user = result.rows[0];
  if (!user || !(await verifyPassword(password, user.password_hash))) {
    throw new Error("Invalid email or password");
  }
  return {
    user: { id: user.id, email: user.email },
    accessToken: await createAccessToken(user.id)
  };
}

export type AuthenticatedRequest = Request & { userId?: string };

export async function requireAuth(
  request: AuthenticatedRequest,
  response: Response,
  next: NextFunction
) {
  try {
    const header = request.header("authorization");
    if (!header?.startsWith("Bearer ")) {
      response.status(401).json({ error: "Authentication required" });
      return;
    }
    request.userId = await readAccessToken(header.slice("Bearer ".length));
    next();
  } catch {
    response.status(401).json({ error: "Invalid or expired authentication token" });
  }
}
