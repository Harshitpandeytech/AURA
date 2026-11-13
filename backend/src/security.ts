import { createCipheriv, createDecipheriv, randomBytes } from "node:crypto";
import bcrypt from "bcryptjs";
import { SignJWT, jwtVerify } from "jose";
import { config } from "./config.js";

const jwtKey = new TextEncoder().encode(config.JWT_SECRET);
const encryptionKey = Buffer.from(config.TOKEN_ENCRYPTION_KEY, "base64");

if (encryptionKey.length !== 32) {
  throw new Error("TOKEN_ENCRYPTION_KEY must be a base64-encoded 32-byte key");
}

export async function hashPassword(password: string): Promise<string> {
  return bcrypt.hash(password, 12);
}

export async function verifyPassword(password: string, hash: string): Promise<boolean> {
  return bcrypt.compare(password, hash);
}

export async function createAccessToken(userId: string): Promise<string> {
  return new SignJWT({ sub: userId })
    .setProtectedHeader({ alg: "HS256" })
    .setIssuedAt()
    .setExpirationTime("7d")
    .sign(jwtKey);
}

export async function readAccessToken(token: string): Promise<string> {
  const { payload } = await jwtVerify(token, jwtKey);
  if (typeof payload.sub !== "string") throw new Error("Token has no user");
  return payload.sub;
}

export function encryptSecret(value: string): string {
  const iv = randomBytes(12);
  const cipher = createCipheriv("aes-256-gcm", encryptionKey, iv);
  const encrypted = Buffer.concat([cipher.update(value, "utf8"), cipher.final()]);
  return Buffer.concat([iv, cipher.getAuthTag(), encrypted]).toString("base64");
}

export function decryptSecret(value: string): string {
  const payload = Buffer.from(value, "base64");
  const decipher = createDecipheriv("aes-256-gcm", encryptionKey, payload.subarray(0, 12));
  decipher.setAuthTag(payload.subarray(12, 28));
  return Buffer.concat([decipher.update(payload.subarray(28)), decipher.final()]).toString("utf8");
}
