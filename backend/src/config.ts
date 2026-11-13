import "dotenv/config";
import { z } from "zod";

const configSchema = z.object({
  PORT: z.coerce.number().int().positive().default(8080),
  DATABASE_URL: z.string().url(),
  JWT_SECRET: z.string().min(32),
  TOKEN_ENCRYPTION_KEY: z.string().min(32),
  GEMINI_API_KEY: z.string().optional().default(""),
  GEMINI_MODEL: z.string().default("gemini-2.5-flash"),
  GROQ_API_KEY: z.string().optional().default(""),
  GROQ_MODEL: z.string().default("llama-3.3-70b-versatile"),
  GOOGLE_CLIENT_ID: z.string().optional().default(""),
  GOOGLE_CLIENT_SECRET: z.string().optional().default(""),
  GOOGLE_REDIRECT_URI: z.string().url().optional(),
  ANDROID_APP_REDIRECT_URI: z.string().default("com.aurviz.personalagent:/oauth2redirect"),
  CORS_ORIGIN: z.string().optional()
});

export const config = configSchema.parse(process.env);
