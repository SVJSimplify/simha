# Simha (Guardian Angel Assistant)

Android voice companion for seniors living alone (India first): it watches over them, talks with them like a caring family member, and escalates to family when needed. Specs in `docs/`; current round plans in `.claude/software-looper/specs/`.

## Stack
- Kotlin + Jetpack Compose, single `app` module, package `com.simha.app`, minSdk 26, target/compile 35.
- Local-first: Room (all personal data, memory graph), DataStore + SharedPreferences (settings). Voice = Android's own STT/TTS.
- LLM: user-configured, OpenAI-compatible (Settings → AI and voice: base URL, key, model; stored encrypted). Recommended: Sarvam 105B Chat.
- No Accessibility Service. No automating other apps.

## Architecture (care brain, round 5)
- `care/Forms.kt`: task forms (medicine, reminder, symptom, intake) with required slots; `Clock` turns spoken times ("5") into HH:MM.
- `care/Checks.kt`: fixed, profile-aware checks (water, sleep, pain, mood, meals; fluid-restricted conditions reverse water advice). The AI can't override them.
- `care/CareEngine.kt`: runs `note_task` calls → fills forms → asks the one missing question (string templates) or executes; builds the situation brief (exact time, time-since facts, heat, flags, open tasks).
- `care/Graph.kt`: local memory graph (`g_node`, `g_edge`), cap 200 nodes, decay/prune daily; conditions, allergies and people never decay. Never synced.
- `care/ToolIndex.kt`: tools linked to trigger words (5 languages), forms and graph types; per turn = core tools + matched ones; `find_tool` loads more.
- `care/Reviewer.kt`: after each conversation, one AI call updates the graph, opens/closes concerns and schedules spoken follow-ups (`Wake.scheduleFollowUp`). Transcripts are not stored.
- `llm/Agent.kt`: if every tool call in a step is `note_task`, the code-composed reply is spoken and no second AI call is made.
- Health → "What Simha knows" lets the person see and delete memory.

## Rules
- Escalation is decided by `RulesEngine` (pure Kotlin, unit-tested). Never by an LLM.
- Danger-word check in code runs before the AI every turn → emergency screen.
- Emergency (112) always via `ACTION_DIAL`. `ACTION_CALL` only for contacts the user approved. Adding contacts or approving calls by voice needs read-back + an explicit yes (`confirmed: true`).
- Reminders and follow-ups speak through Wake (alarm volume over the lock screen), not just notifications. Follow-ups use `WakeReason.Evening` with a `Wake.SAY` extra so they never escalate.
- Every alert, call, permission and recording is audited (local).
- Once a language is chosen, every screen, notification, spoken line and LLM reply is in it. No hardcoded strings; every string in en, hi, te, kn, ta.
- Theme: Dusk (indigo on warm paper, gold accent; `ui/theme`), Baloo per script (Baloo 2 en/hi, Tammudu te, Tamma kn, Thambi ta via `simhaFont(lang)`); Lottie orb (`res/raw/simha_orb.json`, `ui/home/SimhaOrb.kt`); motion pieces in `ui/Motion.kt` (StepBar, SlideToConfirm, TickMark, BlurInText, Waveform, ThinkingText); sliding-pill tab bar. Body ≥ 20sp, targets ≥ 56dp (64dp at fontScale > 1.3), 4dp spacing grid, no exclamation marks.
- Navigation: 3 tabs (Home, Health, Settings); Settings = Me, People, Check-ins, AI and voice, Privacy, About.
- Contacts: "Add family/neighbour/doctor" = label + phone typed in; "Choose from contacts" = pick, then label.

## Backend (Supabase `Simha`, `hvqqagwtkvfpsmammbtb`, ap-south-1)
- `Repo.LOCAL_ONLY = true`: nothing personal is synced. Personal-data tables were dropped.
- Remaining: `caregivers`, `family_messages` (empty, RLS) and storage buckets, kept for the encrypted caregiver relay. `Repo.CAREGIVER_ENABLED = false` (caregiver mode paused).
- Planned backend only: phone-OTP login, payments (Razorpay/Cashfree subscriptions + UPI Autopay), encrypted caregiver relay (~25 KB/user), object storage for client-encrypted audio (R2/B2) with auto-delete.
- Run `get_advisors` after every DDL change.

## CI and releases
- `.github/workflows/android.yml`: typecheck → lint → unit tests → `assembleRelease`; on `main` publishes release `build-N` (versionCode = run number).
- In-app updater (`update/`) installs new `build-N` releases (foreground PackageInstaller; same signing key).
- Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## Branches
- Work on `dev`; push `dev:main` only when CI is green. Push with the SVJSimplify account (repo git config uses `gh auth git-credential`).

## Tools
| Tool | Use for Simha |
|---|---|
| Supabase MCP (project `hvqqagwtkvfpsmammbtb`) | Migrations (`apply_migration`), schema checks, `get_advisors` after every DDL change |
| GitHub CLI `gh` | CI status/logs (`gh run list/view --log-failed`), releases, secrets |
| graphify (`~/.local/bin/graphify.exe`) | Code questions before grep/read; `graphify update .` after code changes (see below) |
| Reticle (`reticle`, global) | Not usable here: it only verifies web/Electron/Tauri apps. Do not run `reticle init` in this repo |
| No local Android SDK | Compile/lint/test only in GitHub Actions; the APK is tested by the user on a phone |
- The user prefers the main session to write the core code itself (no subagents) for engine work; subagents, when used, must be minimal and do exactly the brief.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- When graphify-out/graph.json exists, run `graphify query "<question>"` before your first grep, search or source-file read for a codebase question, and put this rule in every subagent prompt that explores code. Read raw files only for lines the graph or an earlier read already located. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. When the answer is under a Markdown heading, `graphify section "<heading>"` prints that section's text, so the .md need not be opened. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- A committed graphify-out/graph.json is rewritten whole on every build, so a large diff to it in a pull request is expected. Do not flag it as churn or suggest skipping the graph rebuild or its hooks.
