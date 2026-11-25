# Round 5 — Care brain (plan, awaiting go-ahead)

Goal: Simha behaves like a caregiver, not a checklist-taker. Reliable, situation-aware, low-token, local-first.

Decisions already made
- Voice: Android's own STT/TTS only (Sarvam voice dropped). LLM: user-configured; recommend Sarvam 105B Chat (~₹13–25/user/month).
- All personal data and memory stay on the phone. Backend later = login (phone OTP), payments (Razorpay/Cashfree subscriptions + UPI Autopay), encrypted caregiver relay, object storage for encrypted audio.
- Escalation stays in RulesEngine. The AI can never call anyone or escalate by itself.

## 1. Bug fixes (first)
- Time awareness: every turn gets exact date, time, weekday, and "time since" facts (last dose, last check-in, last talk).
- Listening: longer end-of-speech silence, keep partial words on pause, don't cut the user off, no early stop.
- Random resets: investigate first (recognizer error loop, state lost on screen-off, process death); fix the real cause; conversation state survives screen-off and short app switches.

## 2. Task-form engine (checklists in code)
- The AI's job each turn: extract tasks via tool calls (several per utterance allowed) and phrase short replies. Code decides everything else.
- Each task type is a form in code with required slots + validators:

| Task | Slots | Validators |
|---|---|---|
| Medicine | name, dose, times | am/pm from context; allergy clash warning |
| Reminder | what, time, repeat? | future time; ask repeat only if unclear |
| Add contact | label, role, number | Indian number; read-back + yes |
| Symptom | what, since, severity | danger words → first aid; rising → concern |
| Water / sleep / meals / mood / pain | amount/level | profile-aware fixed checks (see 3) |
| Call | who | contacts match; unapproved → dialer |

- Complete form → save, schedule, short confirmation. Missing slot → code picks the one correct question (3–4 natural variants per language). Multiple tasks handled in order. Topic switch → form waits and resumes later.
- Free chat (no task) → one normal AI call with a tiny persona prompt (no rule list).
- Danger-word check in code runs before the AI every turn.

## 3. Situation brief + fixed checks
- Built in code before every reply (~10–15 lines): time and routine, today's signal (doses, water, sleep, mood, pain, check-ins), weather/heat, open concerns, recent patterns, top 3 ranked concerns.
- Fixed checks (code, profile-aware), e.g.: water < ~1.5 L by evening (< 2 L on heat days) → caring follow-up, unless a fluid-restriction condition (heart failure, kidney disease) is in the profile; sleep < 5 h; dose late or skipped twice; pain higher than yesterday. Checks can't be overridden by the AI.
- Patterns over days from daily/weekly signal lines: poor sleep streaks, skipped doses, falling mood, rising pain, less activity → gentle mention + earlier check-in (never escalation).

## 4. Local graph: memory + tools
- Two compact Room tables: nodes (id, type byte, label, importance, ts) and edges (src, dst, relation byte, strength, ts). Cap ~200 nodes / ~500 edges (~30–50 KB). Decay and prune; allergies, conditions and family never decay. Never synced.
- Memory nodes: people, conditions, medicines, routines, places, concerns, events. Built by the background reviewer (5); duplicates merged ("Priya" = "my daughter" = "beti").
- Retrieval per turn: seeds from the utterance + situation brief + open forms → 1–2 hops → top lines by strength × recency → ~10–20 lines.
- Tool nodes: tools linked to concepts, trigger words in 5 languages, memory nodes and forms. Per turn: always-on core (emergency, first aid, call contact, reminder, find_tool) + 3–8 retrieved tools. `find_tool(query)` lets the model load anything missing. Misses logged to improve triggers.
- Health → "What Simha knows": a simple list view to see and correct/delete what Simha believes.

## 5. Background reviewer + follow-ups
- After each conversation (and every ~10 turns): one cheap AI call reviews the transcript against the brief → updates graph, opens/closes concerns, catches missed tasks, schedules follow-ups.
- Concerns carry a check-back time; Wake fires a follow-up ("You had a headache this morning — how is it now?"). Resolved concerns close.
- Transcripts are discarded after review.

## 6. Local-first data move
- Supabase stops storing profile, contacts, medicines, signals, facts, events, audit, recordings. All of these live in Room only.
- Caregiver features paused until the encrypted relay round (backend later). Alerts and emergency calls keep working phone-to-phone (SMS/dialer).
- Existing Supabase user data is exported to the phone once, then deleted from the server (with user confirmation).

## Cost and latency targets
- ~1 AI call per turn (2 on health/safety turns), reply starts < 2 s after speech ends.
- Prompt per turn < ~2.5k tokens (cached persona + brief + graph lines + 8–12 tools).

## Tests
- Unit: every form, validator and fixed check; brief builder; graph retrieval, decay, merge; tool retrieval; danger-word gate.
- Scenario set (~20) runnable from Settings → AI and voice against the user's model: heat + dizziness, missed dose after a fall, "I drank 1 litre", lonely evening, scam call, third bad night, two tasks in one sentence, topic switch mid-form, unknown neighbour, etc.

## Build order and agents (Sonnet, minimal)
1. Bug fixes (time, listening, resets).
2. Task-form engine + danger gate + fixed checks + brief (shared contracts first).
3. Graph (memory + tools) + retrieval + "What Simha knows" view.
4. Background reviewer + follow-ups.
5. Local-first move.
6. CI green → release on main (in-app update).

## Open questions for you
- Delete server copies of existing data after export? (default: yes, with a one-time confirmation)
- Pause caregiver mode until the relay round, or keep it on the current backend until then? (default: pause)
