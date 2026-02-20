# Release Notes — v{VERSION}

> **Date:** {YYYY-MM-DD}
> **Tag:** `v{VERSION}`
> **Deploy SHA:** `{git-sha}`

---

## 🎯 Summary

{One-paragraph description of what this release delivers.}

---

## ✨ New Features

- **{Feature Name}** — {Brief description} ([#PR](link))
- **{Feature Name}** — {Brief description} ([#PR](link))

## 🐛 Bug Fixes

- Fixed {issue description} ([#Issue](link))

## 🔧 Improvements

- {Performance, refactor, DX improvement} ([#PR](link))

## 🗄️ Database Migrations

| Version | Description | Reversible? |
|---------|-------------|-------------|
| V{N} | {description} | No (backup first) |

## ⚠️ Breaking Changes

- {Description of breaking change and migration path}
- None in this release.

## 📋 Environment Variable Changes

| Variable | Action | Default | Notes |
|----------|--------|---------|-------|
| `{VAR_NAME}` | Added | `{default}` | {purpose} |

---

## 🚀 Deploy Steps

1. Backup database: `./ops/backup-db.sh`
2. Pull latest images: `docker compose -f docker-compose.prod.yml pull`
3. Deploy: `docker compose -f docker-compose.prod.yml up -d`
4. Run post-deploy checks: see [post_deploy_checks.md](post_deploy_checks.md)
5. Monitor for 15 minutes

## ↩️ Rollback

```bash
# If issues are found:
./ops/backup-db.sh  # safety backup of current state
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d \
  -e TAG=<previous-tag> --no-deps backend
./ops/healthcheck.sh
```

---

## 📊 Metrics to Watch Post-Deploy

- [ ] `/actuator/health` returns `UP`
- [ ] Error rate < 1% (check logs)
- [ ] p95 latency < 500ms
- [ ] Payment success rate > 99%
- [ ] WebSocket active connections stable

---

*Template version: 1.0 — Update as release process evolves.*
