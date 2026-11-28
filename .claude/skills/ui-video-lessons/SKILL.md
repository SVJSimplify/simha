---
name: ui-video-lessons
description: Use when designing, redesigning, or critiquing any Simha screen (Home, Talk, Onboarding, Settings, Permissions, Health) or any new UI. Applies lessons distilled from three product-design videos: genre-first design, signature moments, designed empty/error/win states, recovery over perfection, time-to-first-value, one design-language file. Pairs with the Dusk theme and senior-accessibility rules in CLAUDE.md.
---

# UI video lessons (Simha)

Source: three design talks on premium-looking apps (genre rules, retention and onboarding, design systems for AI-built apps). Paraphrased; no video text is reproduced here.

## Principles

1. **Pick the genre first.** Quiet tool, character/habit, atmosphere, data, or ambient (widget/notification-first). Simha is a care companion: character-led and calm. Design to that genre's rules, not to "generic AI app".
2. **Calm base, one signature moment.** Keep chrome quiet (one accent, plenty of space, type does the work). Give each main flow one moment people remember (the orb's response, the check-in confirmation).
3. **Design the moments that AI and templates skip.** Empty, error, waiting, undo, win. Each gets a real designed state, never a bare spinner or "Something went wrong".
4. **Show what happened and what to do next.** Every state answers: what changed, what can I undo, what do I do now.
5. **Recovery beats perfection.** A failed step with a clear, warm fix builds more trust than a flawless step with no explanation.
6. **Time to first value.** Get the user to one real success fast. Move long setup (permissions, profile) after the first useful moment, or split it into small steps.
7. **One hero per screen.** One big number or one main action. Secondary things are smaller and quieter.
8. **Character needs the person.** Any mascot/orb must react to real user state (time of day, check-ins, streak-like care), not sit as decoration.
9. **Reminders are human and specific.** Name the thing ("Your 8 am tablet") and the time. Stop nagging after repeated ignores, and say so kindly.
10. **Consistency is the feature.** One font family, one icon set, one spacing scale, one radius set, one motion curve, used on every screen. Inconsistency reads as cheap.
11. **Motion settles like objects.** Short, eased, never blocking. Respect reduced-motion.
12. **Write a design-language file first.** Extract tokens (colour, type, spacing, corners, motion, voice) once; every new screen reads that file before it is built.

## Process (per screen)

1. Name the screen's one job and its genre rule.
2. List its states: default, empty, loading, error, success, offline, permission-denied, undo.
3. Pick the hero element (one number or one action) and its size.
4. Lay out with the 4dp grid; check every tap target is at least 56dp (64dp when font scale > 1.3).
5. Set type: body at least 20sp; headings in Baloo; one emphasis weight per block.
6. Set colour from Dusk tokens only (#28356B primary, #E8A33D gold accent, #F7F2EA page). Check contrast at 4.5:1 for body text.
7. Write copy in the user's language, short, warm, no exclamation marks, one instruction per line.
8. Add one signature moment, or confirm the screen intentionally has none.
9. Check the screen at font scale 1.3 and in 375dp width.
10. Compare against the design-language file; fix any token drift.

## Checklist

- [ ] One job, one hero, one primary action
- [ ] Empty, error, success, undo states designed
- [ ] Every error says what happened, what is safe, and the next step
- [ ] Targets >= 56dp, body text >= 20sp, 4dp spacing grid
- [ ] Only Dusk tokens; one accent colour per screen
- [ ] Motion short, eased, reduced-motion respected
- [ ] Reminders name the thing and the time
- [ ] Mascot/orb state reflects real user state
- [ ] Onboarding reaches a first success before any long form

## Do / Don't

| Do | Don't |
|---|---|
| Show a clear "what changed + undo" after actions | Show a silent success or a generic error |
| Put the key number big and alone | Put four equal stat cards on one screen |
| Use one accent gold for the single most important thing | Decorate with gradients, glows, emoji labels |
| Ask for permissions at the moment they are needed, with the reason | Stack five permission prompts before first use |
| Use plain, human copy | Use jargon, all-caps labels, or "Access denied" |
| Keep one icon style and one radius set | Mix filled and outline icons, or several radii |
| Let the orb react to care events | Use the orb as a static logo |

## Simha-specific notes

- Talk/conversation: show the listening, thinking and speaking states as clear, large cues; always show what Simha will do next.
- Onboarding: first screen promises one benefit in one sentence; first success (a reminder set or a check-in) before any optional setup.
- Permissions hub: each permission card says what it enables for this person and what happens if they say no.
- Health: one hero status per view; memory items show what Simha knows and how to delete it.
- Settings: group by person intent (Me, People, Check-ins, AI and voice, Privacy, About); no more than one level deep.
