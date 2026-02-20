#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────
# QuickBite — Restore PostgreSQL database from a backup
# Usage:   ./ops/restore-db.sh backups/quickbite_20250101_120000.sql.gz
# Env:     DB_HOST, DB_PORT, DB_NAME, DB_USERNAME (or defaults)
# ─────────────────────────────────────────────────────────────────
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: $0 <backup-file.sql.gz>"
  echo "Example: $0 backups/quickbite_20250101_120000.sql.gz"
  exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "$BACKUP_FILE" ]; then
  echo "❌ Backup file not found: $BACKUP_FILE"
  exit 1
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-quickbite}"
DB_USER="${DB_USERNAME:-quickbite}"

echo "⚠️  WARNING: This will overwrite the '${DB_NAME}' database on ${DB_HOST}:${DB_PORT}."
echo "    Backup file: ${BACKUP_FILE}"
read -rp "Continue? (y/N): " CONFIRM
if [[ "$CONFIRM" != [yY] ]]; then
  echo "Aborted."
  exit 0
fi

echo "🔄 Restoring ${BACKUP_FILE} → ${DB_NAME}@${DB_HOST}:${DB_PORT}..."

# Drop and recreate to start clean
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres \
  -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${DB_NAME}' AND pid <> pg_backend_pid();" \
  2>/dev/null || true

psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres \
  -c "DROP DATABASE IF EXISTS ${DB_NAME};"

psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres \
  -c "CREATE DATABASE ${DB_NAME};"

# Restore
zcat "$BACKUP_FILE" | psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" --quiet

echo "✅ Restore complete."
echo "   Flyway will reconcile migrations on next backend startup."
