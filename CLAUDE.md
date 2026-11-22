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
- Work on `dev`; `main` receives merges.
