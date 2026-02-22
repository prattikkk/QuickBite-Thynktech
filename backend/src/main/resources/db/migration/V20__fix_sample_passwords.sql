-- V20__fix_sample_passwords.sql
-- Fix sample user passwords: V3 seeded BCrypt hash for 'password123',
-- but Login.tsx displays 'Test@1234' as the credential.
-- This migration updates all @quickbite.test sample users so that
-- fresh deployments work out-of-the-box with the UI-displayed password.
--
-- BCrypt hash for 'Test@1234':
--   $2a$10$maNiaEtOZzJupIB9rVzxf.RNmNNfu3P0gxUeewM/tio5WVYrcWzXO

UPDATE users
SET    password_hash = '$2a$10$maNiaEtOZzJupIB9rVzxf.RNmNNfu3P0gxUeewM/tio5WVYrcWzXO'
WHERE  email LIKE '%@quickbite.test'
  AND  password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldMqU0rCuC2BfDVDGpG';
