# Simha — Handoff Prompt

Paste this into a new Claude Code session opened in `D:\Simha`. Read `CLAUDE.md` first; it is the source of truth for architecture and rules. This file adds history, failures, next steps and how the owner likes to work.

---

## 1. Project in one paragraph

Simha ("Guardian Angel Assistant") is an Android app (Kotlin + Jetpack Compose, minSdk 26) for seniors in India who live alone. It is a hands-free voice companion that checks in on them by speaking aloud at set times, remembers their life, guides them through health situations with fixed first-aid steps, reminds them about medicines, and escalates to family through fixed rules (never the AI). Five languages: English, Hindi, Telugu, Kannada, Tamil. Everything personal stays on the phone. The AI is whatever OpenAI-compatible model the user configures (recommended: Sarvam 105B Chat). Voice uses Android's own speech recognition and text-to-speech.

- Repo: `SVJSimplify/simha` (public). Work on `dev`; push `dev:main` only after CI is green. `main` publishes release `build-N`; the app updates itself in-app.
- Latest release: **build 35** (care brain). `CLAUDE.md` updated after it (commit `5708f60`, on `dev` only).
- Supabase project `Simha` (`hvqqagwtkvfpsmammbtb`, ap-south-1): only `caregivers` and `family_messages` remain (empty, RLS on); personal-data tables were dropped; security advisors clean.
- Specs: `docs/` (original product, technical, onboarding and UI specs), `.claude/software-looper/specs/round5-care-brain.md` (last round's plan).
- Work files (briefs, outputs, screenshots): `D:\Claude\Simha\{briefs,ref,out,screenshots}`.

## 2. What was built, round by round

1. **First build**: Soft Sand theme, onboarding, check-ins, RulesEngine, LLM tools, caregiver mode, CI + signed APK releases. Supabase schema with RLS.
2. **Rose clay redesign**: mandatory onboarding fields (language, name, at least one valid Indian phone contact, approvals), full 5-language UI, health memory, Sarvam STT/TTS + voice-agent calls (later dropped).
3. **Guardian round**: first-aid guides (16 situations), spoken wake-ups at alarm volume over the lock screen, fall detection, night watch, learned routine, heat/AQI notes, safe-area alert, medicines screen, medical ID, family voice messages, caregiver summaries, in-app updater.
4. **Polish rounds**: 3D orb, then recoloured Sunrise gold, then flat 2D ChatGPT-style orb; 3-tab navigation; Settings in 6 pages; one-step permissions; motion everywhere; older-Android parity; lean data (daily/weekly signal tokens, 30-fact cap); live contacts in context; unslop pass (calmer Home, 4dp grid, no exclamation marks).
5. **Care brain (build 35)**, written by the main session itself: task-form engine, fixed profile-aware checks, situation brief with exact time, local memory graph (nodes/edges, decay, cap 200), tool graph with per-turn loading and `find_tool`, background reviewer that schedules spoken follow-ups, listening fixes, local-only data, caregiver mode paused, "What Simha knows" screen.

## 3. What was tried and what failed (learn from these)

| Attempt | What happened | Lesson |
|---|---|---|
| Check-ins as notifications | Seniors ignore notifications; owner saw only a reminder, no voice | Every check-in, reminder and follow-up must **speak** via Wake (alarm volume, full-screen, overlay permission) |
| Full-screen intent alone to open the wake screen | Android shows it as a plain heads-up when unlocked, and Android 14 sideloads often lack the permission | Speak first from the app process; start the activity directly when "Appear on screen" is granted; readiness card and test button in Settings |
| Language change via `AppCompatDelegate.setApplicationLocales` | Black flash. Declaring `configChanges=locale` did **not** fix it | Language is applied in-app (`LocaleCtx` + Compose `LocalContext`/`LocalConfiguration`), no system locale switch |
| "Update now" never cleared | Install confirm intent was launched from a background receiver; Android 10+ blocks it, so nothing installed | Launch the confirm intent from the foreground banner. The owner had to install one build by hand to get the fix |
| Prompt-only "rules" for the AI | Simha acted like a dumb checklist: accepted "1 litre of water", asked generic medicine questions, kept saying "one moment", said it didn't know first aid | Rules live in code: task forms, fixed checks, danger gate, data-aware prompt. The AI extracts and phrases only |
| Context built once per conversation | A neighbour added mid-conversation was unknown to Simha | Rebuild context before every model request; list contacts (never numbers) |
| 3D orb (AGSL shader, then OpenGL) | Owner: "too 3D", "looks weird" | Flat 2D Canvas orb with drifting colour clouds, reacts to voice level |
| Pink/rose orb | Owner disliked pink | Sunrise gold for the orb; Rose clay stays the app theme |
| Sarvam voice (STT/TTS/voice agent) | About ₹510/user/month, 95% of total cost | Android's own voice. Sarvam only as a possible premium later |
| Supabase sync with anonymous sign-in | Anonymous sign-in was never enabled, so nothing ever synced (all tables were empty) | Moot now: app is local-first |
| Subagent-built code | Integration bugs (raw newlines in string literals, contact list missing from prompt, wrong imports) caught only by CI/tests | Owner wants the main session to write engine code itself; treat failing tests as real bugs first |
| `pain-language-engagers` skill | No longer exists in the gooseworks repo | Installed `research-audience` and `write-outreach` instead (user-level skills) |
| Reticle | Only works for web/Electron/Tauri | Do not use for Simha |
| YouPI-PG (self-hosted UPI) | Experimental, SMS-parsing confirmation, no Autopay | Not for real payments |

Still unverified (needs the owner's phone testing): build 35 overall, whether the language flash is truly gone, whether the updater installs in place, wake-up reliability on Xiaomi/Oppo/Vivo/Realme. Hindi/Telugu/Kannada/Tamil text was written by the model and needs native review.

## 4. Next round (agreed direction, not started)

1. **Test feedback on build 35** first. Expect fixes to the care brain (task forms, checks, follow-ups, memory, listening).
2. **Security hardening** (owner said "add both", waiting for his signal): expiring, longer, rate-limited caregiver invite codes; first-launch tester consent screen (what's shared, not for emergencies); API keys off the phone (server function); "Delete my data".
3. **Backend round** (local-first stays):
   - Phone-number OTP login (replaces anonymous auth).
   - Payments: Razorpay or Cashfree subscriptions with UPI Autopay, UPI Intent checkout, webhooks, daily reconciliation. A plan around ₹199/month is viable (AI about ₹13–25/user/month with Android voice + Sarvam 105B Chat; Sarvam's introductory pricing ends at the end of 2026). Hyperswitch only later, at scale.
   - Encrypted caregiver relay (option A): the phone encrypts a daily summary (about 25 KB/user/month, 30-day retention) that only the caregiver's phone can read; rebuild the invite/accept flow; re-enable `Repo.CAREGIVER_ENABLED`.
   - Object storage for client-encrypted audio (Cloudflare R2 or Backblaze B2), Opus 16–24 kbps, random IDs, presigned links from the backend, auto-delete via lifecycle rules. A generous quota inside the subscription; no per-MB billing.
4. **Health Connect** (deferred by the owner): steps, heart rate, sleep, SpO2, BP, glucose; learn baselines; change alerts; store derived tokens only, never raw data.
5. Later ideas: scenario test runner in Settings against the user's model, adaptive routine-based check-ins, medicine-strip photo recognition, better scam check.

## 5. Owner's product preferences

- Simha must feel like a **caring family member**, not a checklist or a reminder app. Situational awareness and common sense matter most.
- **Voice first and proactive**: it speaks first at the right times; seniors don't read notifications.
- **Data-aware**: never act as if it knows data it doesn't have; ask once, then remember.
- **Light and private**: not a medical record; minimal data, compressed tokens, memory graph on the phone only, never on a server. Backend only for login, payments, caregiver relay and audio storage.
- **Cheap to run**: Android's own voice, a cheap capable LLM, low tokens (per-turn tool loading, cached prompt, code-composed replies).
- **Design**: simple, clean, dead easy for seniors, yet premium. Keep the Rose clay theme, Nunito font and current colours; flat 2D animated Sunrise-gold orb (ChatGPT-voice style); animations on buttons, pages and text; no black flashes; identical on old and new Android.
- **Mandatory fields** only where the app needs them to work.
- **Everything localised** in the chosen language, including Settings, notifications and speech.

## 6. Owner's working preferences

- Be concise. Lead with the result. No preamble. Tables over prose for lists.
- For big changes: **draft a plan first and wait for his go-ahead or changes**. Write plans as files when asked. Give honest assessments when he asks "how is my plan" or "is this naive".
- For design choices (colours, fonts, orb styles), show **visual options** and let him choose; he may reject all and ask for more.
- **Write core/engine code yourself; no subagents** unless he says otherwise. When subagents are used, use only the model he names (he has asked for Haiku-only for research, Sonnet for some builds) and tell them to be minimal and do exactly the brief.
- Use **graphify** for code navigation (`~/.local/bin/graphify.exe query "<q>"`), then `graphify update .` after changes.
- Use the **software-looper** skill when he invokes it: CI is the gate; push to `dev`, wait for green, then `dev:main` to release. He installs the APK himself and reports back. Don't test the APK yourself (no Android SDK here).
- He tests on a real phone and reports bluntly; treat each report as the bug list for the next round.
- Push with the SVJSimplify GitHub account. Never push to `main` without green CI on `dev`.
- Keep `CLAUDE.md` current when architecture or rules change.
- He doesn't want to type secrets in chat: tell him exactly where in the app or dashboards to enter keys.
- Destructive actions (dropping tables, deleting data): show counts and ask first; he answers briefly ("Ok sure", "Yes and pause").

## 7. Hooks and environment quirks

- A "Fact-Forcing Gate" hook blocks the first Bash/Edit/Write per file in a session until you state: callers/importers, affected API, data schema, and the user's verbatim instruction. Answer briefly and retry.
- Windows + Git Bash. Python heredocs containing non-ASCII text can break bash quoting; write scripts to the scratchpad and run them with `python -I <file>`.
- No local Android SDK: compile, lint and tests run only in GitHub Actions (`gh run watch`, `gh run view --log-failed`).
- `D:\Claude\Simha\briefs\` holds past round briefs (round 2–4) for reference.
