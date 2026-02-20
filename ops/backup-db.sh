#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────
# QuickBite — Backup PostgreSQL database
# Usage:   ./ops/backup-db.sh
# Env:     DB_HOST, DB_PORT, DB_NAME, DB_USERNAME (or defaults)
# Output:  Timestamped .sql.gz in ./backups/
# ─────────────────────────────────────────────────────────────────
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-quickbite}"
DB_USER="${DB_USERNAME:-quickbite}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
FILENAME="${DB_NAME}_${TIMESTAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"

echo "📦 Backing up ${DB_NAME}@${DB_HOST}:${DB_PORT} → ${BACKUP_DIR}/${FILENAME}"

pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  --no-owner --no-acl --format=plain | gzip > "${BACKUP_DIR}/${FILENAME}"

SIZE=$(du -h "${BACKUP_DIR}/${FILENAME}" | cut -f1)
echo "✅ Backup complete: ${BACKUP_DIR}/${FILENAME} (${SIZE})"

# Retention: keep last 7 daily backups
echo "🧹 Pruning backups older than 7 days..."
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -mtime +7 -delete -print | \
  while read -r f; do echo "  Deleted: $f"; done

echo "🎉 Done."
