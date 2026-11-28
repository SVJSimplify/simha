# Round 6: Alexa-level core (offline first, AI optional)

## Principle
The AI must not be what makes Simha work. Everything that must work (alarms, reminders, medicines, calls, emergencies, phone control, check-ins) runs in code, offline, in all five languages. The AI handles open conversation and the rare request the code cannot parse. With any model (Gemma or better), or with none, the core still works.

```
mic -> patient listener -> [danger gate] -> intent engine (code, offline) --hit--> action + spoken reply (templates)
                                                    \--miss--> AI (online) -> same actions through tools
scheduler (Room + AlarmManager) -> Wake (speaks over lock screen) -> listener (ack / snooze / help)
```

## 1. Patient listening (fixes "doesn't wait", "just yapping")
- End of turn needs about 1.8 to 2.5 s of silence, adapted to the person. After a final result, keep listening for another 2.5 s and join any continuation ("my tablet... the BP one... at 5").
- Barge-in: when the person starts talking, Simha stops talking.
- Replies: one or two sentences, capped in code, not only by the prompt.
- Prefer on-device recognition (`createOnDeviceSpeechRecognizer` on API 31+, `EXTRA_PREFER_OFFLINE` on older versions) with an online fallback.

## 2. Intent engine (code, 5 languages, offline)
- Grammar and keyword frames per language, with slot parsers for time, date, repetition, contact, medicine, number, app and level. It reuses `Clock` and adds `When` for dates and repetition.
- Intents:

| Intent | Example |
|---|---|
| alarm.set / list / cancel | "6 baje ka alarm" |
| reminder.set | Once or repeating: "remind me every Monday to pay the bill" |
| medicine.add / taken / skipped / list | |
| call.contact / call.112 / sms.family | |
| emergency | |
| phone.volume / flashlight / brightness / dnd / open_app / play_music / time / date / battery | |
| checkin.fine / help | |
| messages.play | |
| cancel / repeat / stop | |

- Any missing piece of information becomes one spoken question, and the answer is filled in by code. A form expires after 20 minutes.
- Before acting, Simha reads back anything that can't be undone ("Alarm for 6 am every day. Shall I set it?"). Only a clear yes does it.
- Test target: more than 300 transcript tests, at least 60 per language, including pauses, fillers and mixed Hindi-English. CI fails if any case regresses.

## 3. Unified scheduler
- One `Task` table: kind (alarm, reminder, medicine, checkin), what, time, repetition (once, daily, weekdays, set days, every N hours, monthly), next fire time, active, snooze.
- Each task fires through an exact alarm. Simha speaks it at alarm volume over the lock screen and listens for "done", "snooze 10 minutes" or "help".
- An unanswered medicine or check-in is retried twice and then escalated by RulesEngine.
- Everything is rescheduled after a reboot, a time change or an app update. A daily self-check repairs any missing alarms.

## 4. Emergency (code only, works offline)
- Triggers: danger words in 5 languages, a detected fall with no answer, the help button, a spoken "help", or an unanswered check-in plus a missed medicine.
- Actions are decided by RulesEngine, never the AI, and all start at once:
  1. Call approved family automatically, starting with the first, on speakerphone.
  2. Send all family an SMS with the location (GPS, with a maps link).
  3. Open the dialer with 112 ready. Android does not let apps call emergency numbers on their own, so this needs one big button press. Simha says "Press the green button".
  4. Speak fixed first-aid steps for the situation. For chest pain: sit down and stay still, unlock the door, and take the aspirin only if their doctor has approved it. Never "walk to the hospital".
  5. Turn the screen and volume to maximum, and keep the screen on.
- Works with no internet: calls and SMS use the mobile network.

## 5. One-tap setup, plus a Permissions screen
- An "Allow everything Simha needs" button is confirmed with a fingerprint, face or phone PIN (BiometricPrompt with device credential). It then:
  - Asks for all normal permissions together: microphone, phone, SMS, contacts, location, notifications, camera and activity.
  - Steps through each special permission with one spoken sentence each: appear on top, exact alarms, battery unrestricted, full-screen alerts, Do Not Disturb access, changing system settings for brightness, and the phone maker's autostart screen.
- A Permissions screen shows each item as a plain description with an on/off switch and a "Why" line. Turning one off explains what stops working.

## 6. Phone control (no root)
| Ability | How |
|---|---|
| Volume | AudioManager |
| Flashlight | CameraManager |
| Brightness | WRITE_SETTINGS |
| Do Not Disturb | Notification policy access |
| Open an app | Launch intent by spoken name, matched to installed app names |
| Music | Local songs (MediaStore), or open YouTube or radio by search |
| Calls, SMS, WhatsApp call or chat | Intents to known contacts |
| Alarms | Simha's own scheduler, so it can speak them |

## 7. Online and offline
| Area | Offline | Online |
|---|---|---|
| Intents, scheduler, emergency, calls, SMS, phone control | Yes, all of it | Yes |
| Speech | On-device | Better accuracy |
| AI chat | Simha says it needs internet for that and offers what it can do | Yes |
| On-device AI (optional, later) | Gemma 3 1B through MediaPipe LLM Inference, about 550 MB downloaded once over Wi-Fi | |

## 8. AI layer (already being fixed)
- Works with any OpenAI-compatible model.
- If a model rejects tools, Simha falls back to tool calls written as text. If it rejects the system prompt, the instructions go into the first message instead.
- Thinking text, JSON and markdown are never spoken.
- Every error is spoken in the person's language. A single error never ends the conversation.

## 9. Quality gates
- Unit tests:
  - Intent transcripts (5 languages)
  - Clock and When parsing
  - Scheduler next-fire with a fake clock, including daylight-saving changes and reboot
  - RulesEngine
  - Emergency decision table
  - AI compatibility transforms
- CI must pass before release. You then test on a real phone.

## Decisions needed
1. **Accessibility service.** Lets Simha read the screen and press buttons in other apps (WhatsApp video call, reading messages aloud). This lifts the current "no Accessibility" rule. It's powerful, but Play Store review is strict.
2. **Automatic family calls in an emergency** with no confirmation question (recommended: yes, with a 10-second spoken countdown the person can cancel).
3. **On-device Gemma** (about 550 MB) now, or later.

## Order of work
1. Patient listener, intent engine, scheduler, emergency.
2. Setup and the Permissions screen.
3. Phone control.
4. Finish the AI-layer fixes.
5. Tests, CI, release.

## Decisions (owner, 2026-11-28)
1. Accessibility service: **add it**.
2. Automatic family calls in an emergency: **yes**, with a 10-second spoken countdown the person can cancel.
3. On-device AI: **none**. No local model on people's phones.

## Undo for every automatic action (owner requirement)
- **ActionJournal:** each action records its effect and how to reverse it.
  - Volume, brightness and Do Not Disturb: back to the earlier level.
  - Flashlight: off.
  - An alarm, reminder or medicine that was added: deleted.
  - One that was deleted: restored.
  - A contact that was added: removed.
  - An Accessibility action: the Back button.
- **Saying "undo", "cancel that" or "wait no"** (in 5 languages), or tapping the Undo snackbar for 10 s, reverses the last action. The journal keeps the last 20 actions, and undoing more than one goes back through them in order.
- **Actions that can't be reversed** (a call, an SMS, a tap in another app) start with a spoken countdown: 5 s, or 10 s in an emergency. Saying "stop" cancels it.
