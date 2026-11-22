# Guardian Angel Assistant: Technical Specification

## 1. Stack

| Layer | Choice | Notes |
|---|---|---|
| Backend | Supabase, region ap-south-1 (Mumbai) | Row-level security on all medical data |
| Client | Kotlin, Jetpack Compose (Android) | Android first |
| Local storage | Room (structured data), DataStore (settings) | Offline logs and profile |
| Sync | PowerSync (verify production status before use) | Fallback: custom sync layer |
| Voice agent | Sarvam Voice Agents | Speech, calls, 11 Indian languages plus English |
| Phone numbers | Pool of provider numbers in a Sarvam group | Retry rotation enabled |
| LLM | Small model with tool calling (select by test) | Conversation and summaries only |
| Alerts | FCM push; SMS or WhatsApp text | Family and neighbors |

## 2. Components

### 2.1 Activity Monitor
- Uses usage access, which the user enables manually.
- Sends a heartbeat to the server.
- Runs as a foreground service to survive battery optimization.
- If usage access is denied, falls back to scheduled check-ins and states the limitation to the user.

### 2.2 Rules Engine
- Deterministic. Decides when to check in, escalate, and call.
- The language model never decides escalation.
- Every rule has a configurable limit and a documented owner.

### 2.3 Call Manager

| Call type | Method | User involvement |
|---|---|---|
| Emergency (112, ambulance) | Dialer intent (ACTION_DIAL) | User presses call |
| Approved family or hospital call | ACTION_CALL with CALL_PHONE permission | Pre-approved during onboarding; revocable |
| Agent call | Outbound from a provider number in the pool | Pre-approved; receiver hears AI disclosure |
| One-tap join | User starts call, approves join, carrier merge | User approves each join |

Rules:
- Non-system apps cannot place emergency calls or reformat emergency numbers. Emergency calls always use the dialer path.
- If a merge fails, the user stays on the call and the app shows the failure.
- Log every call with time, number, reason, and outcome.

### 2.4 Voice Agent
- Sarvam Voice Agents deploys the agent on a provider number pool.
- One agent per purpose: check-in, family escalation, hospital inquiry.
- Opening line discloses that an automated assistant calls for the user.
- Pool rotation: if a number fails or is busy, use another.
- Concurrency is limited by plan. Pay as you go allows 4 concurrent calls. Use Business or Enterprise for scale.

### 2.5 Recording
- Notify the person at the start of every recording.
- Store encrypted. Retention set by the user, with a default.
- Recording on Wi-Fi calls is unreliable on some phones. Test on target devices.

### 2.6 Audit Log
- Records every alert, call, permission use, and recording.
- Viewable by the user and by approved caregivers.

## 3. Permissions

| Permission | Purpose | Asked when | Revocable |
|---|---|---|---|
| Usage access | Activity monitor | Onboarding, explained plainly | Yes |
| CALL_PHONE | Approved calls | Onboarding, only if user approves calls | Yes |
| Microphone | Voice check-ins and recordings | Onboarding | Yes |
| Location | Alerts only | Onboarding, can decline | Yes |
| Notifications | Check-ins and alerts | Onboarding | Yes |
| Contacts (system picker) | Choose emergency contacts | When needed | Per use |

The app honors every denial without pressure. It offers an alternative where one exists.

## 4. Compliance

### 4.1 Google Play
- Each sensitive permission is tied to a core feature listed in the Play listing.
- Users can revoke each permission.
- No Accessibility Service use in this version.

### 4.2 DPDP Act, 2023 and Rules, 2025
- Consent is free, specific, informed, unconditional, and unambiguous.
- Breach reporting within 72 hours.
- Core provisions take effect around 13 May 2027. Build to that standard now.
- Health data has no separate category under the Act. Treat it as sensitive in design anyway.

### 4.3 Recording and AI Disclosure
- Get legal review before launch on recording consent and AI disclosure in India.
- Disclose at the start of every recording and every agent call.

## 5. Data Model (Summary)

- `users` (profile, language, consent records)
- `emergency_contacts` (name, number, role, approval status)
- `medical_profile` (conditions, medicines, allergies, doctor)
- `rules` (activity limits, escalation steps)
- `events` (heartbeats, check-ins, alerts)
- `calls` (type, number, reason, outcome, recording reference)
- `recordings` (encrypted reference, retention date, consent record)
- `audit_log` (every action, time, actor)

Row-level security: only the user and approved caregivers read their own rows.

## 6. Open Items

- Verify PowerSync production status.
- Test one-tap merge on Jio, Airtel, and Vi.
- Select the LLM by tool-call accuracy and latency tests.
- Test Wi-Fi call recording on target devices.
- Get legal review on recording and AI disclosure.
- Choose plan tier for expected call volume.
