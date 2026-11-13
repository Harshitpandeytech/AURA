-- CreateSchema
CREATE SCHEMA IF NOT EXISTS "public";

-- CreateEnum
CREATE TYPE "ScheduledEmailStatus" AS ENUM ('SCHEDULED', 'PROCESSING', 'UPDATING', 'SENT', 'FAILED', 'CANCELLED', 'UNKNOWN');

-- CreateTable
CREATE TABLE "users" (
    "id" UUID NOT NULL,
    "email" TEXT NOT NULL,
    "password_hash" TEXT NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "users_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "gmail_accounts" (
    "id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "email" TEXT NOT NULL,
    "access_token_encrypted" TEXT,
    "refresh_token_encrypted" TEXT,
    "disconnectedAt" TIMESTAMP(3),
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "gmail_accounts_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "app_auth_codes" (
    "id" UUID NOT NULL,
    "codeHash" TEXT NOT NULL,
    "account_id" UUID NOT NULL,
    "expires_at" TIMESTAMP(3) NOT NULL,
    "used_at" TIMESTAMP(3),
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "app_auth_codes_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "device_sessions" (
    "id" UUID NOT NULL,
    "token_hash" TEXT NOT NULL,
    "account_id" UUID NOT NULL,
    "expires_at" TIMESTAMP(3) NOT NULL,
    "revoked_at" TIMESTAMP(3),
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "device_sessions_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "oauth_states" (
    "state" TEXT NOT NULL,
    "user_id" UUID NOT NULL,
    "expires_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "oauth_states_pkey" PRIMARY KEY ("state")
);

-- CreateTable
CREATE TABLE "scheduled_emails" (
    "id" UUID NOT NULL,
    "account_id" UUID NOT NULL,
    "recipient" TEXT NOT NULL,
    "subject" TEXT NOT NULL,
    "body" TEXT NOT NULL,
    "scheduled_at" TIMESTAMP(3) NOT NULL,
    "time_zone" TEXT NOT NULL,
    "status" "ScheduledEmailStatus" NOT NULL DEFAULT 'SCHEDULED',
    "idempotency_key" TEXT NOT NULL,
    "pg_boss_job_id" TEXT,
    "job_version" INTEGER NOT NULL DEFAULT 1,
    "message_id" TEXT,
    "attempts" INTEGER NOT NULL DEFAULT 0,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "execution_at" TIMESTAMP(3),
    "last_error" VARCHAR(500),

    CONSTRAINT "scheduled_emails_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "email_executions" (
    "id" UUID NOT NULL,
    "scheduled_email_id" UUID NOT NULL,
    "attempt" INTEGER NOT NULL,
    "status" "ScheduledEmailStatus" NOT NULL,
    "error_details" VARCHAR(500),
    "gmail_message_id" TEXT,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "email_executions_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "memories" (
    "id" BIGSERIAL NOT NULL,
    "user_id" UUID NOT NULL,
    "key" TEXT NOT NULL,
    "value" TEXT NOT NULL,
    "category" TEXT NOT NULL DEFAULT 'general',
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "memories_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "scheduled_jobs" (
    "id" BIGSERIAL NOT NULL,
    "user_id" UUID NOT NULL,
    "instruction" TEXT NOT NULL,
    "next_run_at" TIMESTAMP(3) NOT NULL,
    "interval_seconds" INTEGER NOT NULL DEFAULT 86400,
    "enabled" BOOLEAN NOT NULL DEFAULT true,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "scheduled_jobs_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "scheduled_job_runs" (
    "id" BIGSERIAL NOT NULL,
    "job_id" BIGINT NOT NULL,
    "status" TEXT NOT NULL,
    "result" TEXT,
    "error" TEXT,
    "started_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "completed_at" TIMESTAMP(3),

    CONSTRAINT "scheduled_job_runs_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "users_email_key" ON "users"("email");

-- CreateIndex
CREATE UNIQUE INDEX "gmail_accounts_user_id_key" ON "gmail_accounts"("user_id");

-- CreateIndex
CREATE UNIQUE INDEX "gmail_accounts_email_key" ON "gmail_accounts"("email");

-- CreateIndex
CREATE UNIQUE INDEX "app_auth_codes_codeHash_key" ON "app_auth_codes"("codeHash");

-- CreateIndex
CREATE INDEX "app_auth_codes_expires_at_idx" ON "app_auth_codes"("expires_at");

-- CreateIndex
CREATE UNIQUE INDEX "device_sessions_token_hash_key" ON "device_sessions"("token_hash");

-- CreateIndex
CREATE INDEX "device_sessions_account_id_expires_at_idx" ON "device_sessions"("account_id", "expires_at");

-- CreateIndex
CREATE INDEX "oauth_states_expires_at_idx" ON "oauth_states"("expires_at");

-- CreateIndex
CREATE UNIQUE INDEX "scheduled_emails_idempotency_key_key" ON "scheduled_emails"("idempotency_key");

-- CreateIndex
CREATE INDEX "scheduled_emails_account_id_created_at_idx" ON "scheduled_emails"("account_id", "created_at");

-- CreateIndex
CREATE INDEX "scheduled_emails_account_id_status_scheduled_at_idx" ON "scheduled_emails"("account_id", "status", "scheduled_at");

-- CreateIndex
CREATE INDEX "email_executions_scheduled_email_id_created_at_idx" ON "email_executions"("scheduled_email_id", "created_at");

-- CreateIndex
CREATE UNIQUE INDEX "email_executions_scheduled_email_id_attempt_key" ON "email_executions"("scheduled_email_id", "attempt");

-- CreateIndex
CREATE INDEX "memories_user_id_updated_at_idx" ON "memories"("user_id", "updated_at");

-- CreateIndex
CREATE UNIQUE INDEX "memories_user_id_key_key" ON "memories"("user_id", "key");

-- CreateIndex
CREATE INDEX "scheduled_jobs_enabled_next_run_at_idx" ON "scheduled_jobs"("enabled", "next_run_at");

-- CreateIndex
CREATE INDEX "scheduled_job_runs_job_id_started_at_idx" ON "scheduled_job_runs"("job_id", "started_at");

-- AddForeignKey
ALTER TABLE "gmail_accounts" ADD CONSTRAINT "gmail_accounts_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "app_auth_codes" ADD CONSTRAINT "app_auth_codes_account_id_fkey" FOREIGN KEY ("account_id") REFERENCES "gmail_accounts"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "device_sessions" ADD CONSTRAINT "device_sessions_account_id_fkey" FOREIGN KEY ("account_id") REFERENCES "gmail_accounts"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "oauth_states" ADD CONSTRAINT "oauth_states_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "scheduled_emails" ADD CONSTRAINT "scheduled_emails_account_id_fkey" FOREIGN KEY ("account_id") REFERENCES "gmail_accounts"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "email_executions" ADD CONSTRAINT "email_executions_scheduled_email_id_fkey" FOREIGN KEY ("scheduled_email_id") REFERENCES "scheduled_emails"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "scheduled_job_runs" ADD CONSTRAINT "scheduled_job_runs_job_id_fkey" FOREIGN KEY ("job_id") REFERENCES "scheduled_jobs"("id") ON DELETE CASCADE ON UPDATE CASCADE;
