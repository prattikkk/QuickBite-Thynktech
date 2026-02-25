-- V22: Add email_verified column to users table
ALTER TABLE users ADD COLUMN email_verified BOOLEAN DEFAULT FALSE;

-- Existing users are considered verified
UPDATE users SET email_verified = TRUE;
