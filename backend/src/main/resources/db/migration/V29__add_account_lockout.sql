-- V29: Add account lockout fields to users table
-- Week 1 Security: Prevent brute force attacks by locking accounts after 5 failed login attempts

ALTER TABLE users
ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS account_locked_until TIMESTAMPTZ NULL;

CREATE INDEX IF NOT EXISTS idx_user_account_locked ON users(account_locked_until);

COMMENT ON COLUMN users.failed_login_attempts IS 'Number of consecutive failed login attempts';
COMMENT ON COLUMN users.account_locked_until IS 'Account locked until this timestamp (15 minutes after 5th failed attempt)';

