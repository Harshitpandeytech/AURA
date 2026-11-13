import { randomUUID } from "node:crypto";
import { pool, withTransaction } from "./db.js";
import { config } from "./config.js";
import { decryptSecret, encryptSecret } from "./security.js";

export async function googleAuthorizationUrl(userId: string): Promise<string> {
  if (!config.GOOGLE_CLIENT_ID || !config.GOOGLE_REDIRECT_URI) {
    throw new Error("Google OAuth is not configured");
  }
  const state = randomUUID();
  await pool.query(
    "INSERT INTO oauth_states (state, user_id, expires_at) VALUES ($1, $2, NOW() + INTERVAL '10 minutes')",
    [state, userId]
  );
  const params = new URLSearchParams({
    client_id: config.GOOGLE_CLIENT_ID,
    redirect_uri: config.GOOGLE_REDIRECT_URI,
    response_type: "code",
    access_type: "offline",
    prompt: "consent",
    scope: "https://www.googleapis.com/auth/gmail.modify",
    state
  });
  return `https://accounts.google.com/o/oauth2/v2/auth?${params}`;
}

export async function completeGoogleAuthorization(code: string, state: string) {
  const stateResult = await pool.query(
    "DELETE FROM oauth_states WHERE state = $1 AND expires_at > NOW() RETURNING user_id",
    [state]
  );
  const userId = stateResult.rows[0]?.user_id;
  if (!userId) throw new Error("Invalid or expired OAuth state");

  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      code,
      client_id: config.GOOGLE_CLIENT_ID,
      client_secret: config.GOOGLE_CLIENT_SECRET,
      redirect_uri: config.GOOGLE_REDIRECT_URI ?? "",
      grant_type: "authorization_code"
    })
  });
  if (!tokenResponse.ok) throw new Error(`Google token exchange failed (${tokenResponse.status})`);
  const tokens = (await tokenResponse.json()) as { access_token?: string; refresh_token?: string };
  if (!tokens.access_token) throw new Error("Google did not return an access token");
  const accessToken = tokens.access_token;

  await withTransaction(async (client) => {
    await client.query(
      `INSERT INTO gmail_accounts (user_id, access_token_encrypted, refresh_token_encrypted)
       VALUES ($1, $2, $3)
       ON CONFLICT (user_id) DO UPDATE SET
         access_token_encrypted = EXCLUDED.access_token_encrypted,
         refresh_token_encrypted = COALESCE(EXCLUDED.refresh_token_encrypted, gmail_accounts.refresh_token_encrypted),
         updated_at = NOW()`,
      [
        userId,
        encryptSecret(accessToken),
        tokens.refresh_token ? encryptSecret(tokens.refresh_token) : null
      ]
    );
  });
  return userId;
}

export async function gmailAccessToken(userId: string): Promise<string> {
  const result = await pool.query(
    "SELECT access_token_encrypted FROM gmail_accounts WHERE user_id = $1",
    [userId]
  );
  const encrypted = result.rows[0]?.access_token_encrypted;
  if (!encrypted) throw new Error("Gmail is not connected");
  return decryptSecret(encrypted);
}
