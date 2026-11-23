# Simha (Guardian Angel Assistant)

Android app for seniors: inactivity watch → check-in → escalation. Specs in `docs/` are the source of truth.

## Stack
- Kotlin + Jetpack Compose, single `app` module, package `com.simha.app`, minSdk 26, target/compile 35.
- Room (local), DataStore (settings), supabase-kt (auth/postgrest/storage) → Supabase project `Simha` (`hvqqagwtkvfpsmammbtb`, ap-south-1).
- Auth: anonymous sign-in (no passwords, per onboarding spec).
- No Accessibility Service. No automating other apps.

## Rules
- REQUIREMENT: every feature works once the user only configures an LLM provider (Settings → AI assistant: base URL, key, model; OpenAI-compatible, stored encrypted on device). Abilities (profile, contacts, status, symptoms, emergency, approved calls, alerts, recording, camera vision) are exposed as tools in `llm/SimhaTools`; every tool call is audited and respects approvals.
- Talk to Simha is a hands-free, human-like voice conversation (auto-listen after speaking, streaming replies, history kept, user's language); it acts via tools and confirms in plain words.
- Onboarding requests every permission with the real system dialog, one per screen.
- Escalation is decided by `RulesEngine` (pure Kotlin, unit-tested). Never by an LLM.
- Emergency (112) always via `ACTION_DIAL`. `ACTION_CALL` only for contacts the user approved.
- Every alert/call/permission/recording → `audit_log`.
- Theme: Soft Sand tokens in `ui/theme/Color.kt`. Body ≥ 20sp, targets ≥ 56dp (64dp at fontScale > 1.3). 200ms fades only.
- Strings in `res/values*/strings.xml` for en, hi, te, kn, ta.

## Database
- Migrations live in Supabase (applied via MCP); mirrored in `supabase/migrations/`.
- RLS on every table; owner or active caregiver via `private.can_access(uuid)`. Caregiver invite via `rpc/accept_invite(code)`.

## CI
- `.github/workflows/android.yml`: lint → unit tests → `assembleRelease` → APK artifact (+ release on `main`).
- Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Optional: `SARVAM_API_KEY`.

## Branches
- Work on `dev`; `main` receives merges. Push with the SVJSimplify account (repo git config uses `gh auth git-credential`).

## Tools
| Tool | Use for Simha |
|---|---|
| Supabase MCP (project `hvqqagwtkvfpsmammbtb`) | Migrations (`apply_migration`), schema checks, `get_advisors` after every DDL change |
| GitHub CLI `gh` | CI status/logs (`gh run list/view --log-failed`), releases, secrets |
| graphify (`~/.local/bin/graphify.exe`) | Code questions before grep/read; `graphify update .` after code changes (see below) |
| Reticle (`reticle`, global) | Not usable here: it only verifies web/Electron/Tauri apps. Do not run `reticle init` in this repo |
| No local Android SDK | Compile/lint/test only in GitHub Actions; the APK is tested by the user on a phone |
- Subagents: Sonnet, told to be minimal and do exactly the brief.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- When graphify-out/graph.json exists, run `graphify query "<question>"` before your first grep, search or source-file read for a codebase question, and put this rule in every subagent prompt that explores code. Read raw files only for lines the graph or an earlier read already located. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. When the answer is under a Markdown heading, `graphify section "<heading>"` prints that section's text, so the .md need not be opened. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- A committed graphify-out/graph.json is rewritten whole on every build, so a large diff to it in a pull request is expected. Do not flag it as churn or suggest skipping the graph rebuild or its hooks.
