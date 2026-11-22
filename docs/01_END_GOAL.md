# Guardian Angel Assistant: End Goal (Product Requirements)

**Product:** Guardian Angel Assistant
**Launch:** India first
**Platform:** Android first
**Users:** Seniors, people living alone, people managing their own health, and family members who watch over someone.

## 1. Purpose

The assistant watches over a person's safety. It checks in, escalates when needed, guides the person through an emergency, and keeps a record. A human always makes the final decision on emergency care.

## 2. Required Capabilities

1. **Activity watch**
   - Track phone activity against a limit set by the user or family (for example, 3 hours with no use).
2. **Check-in**
   - When the limit passes, greet the person by name and ask, "Are you okay?"
   - Wait for a reply.
3. **Escalation**
   - If there is no reply, call the emergency contact.
   - Explain the situation to that contact.
   - The contact decides the next step.
4. **Emergency guidance**
   - During an emergency, give short, calm, step-by-step instructions.
   - Base the instructions on the person's medical profile.
5. **Calls**
   - Emergency services (112) and ambulances: the app opens the dialer. The user presses call.
   - Hospitals, family, and neighbors: the user approves the call in advance. The assistant speaks for the user.
   - Family and neighbors can also receive a text alert with status and location.
6. **Record**
   - Offer to record the person's message.
   - The person chooses to send it to someone or keep it.
   - Save symptom details for the doctor and the family.
7. **Medical profile**
   - Conditions, medicines, allergies, and doctor contact.
   - Editable by the user or an approved caregiver.
8. **Location**
   - Share location only during an alert, or with explicit permission.

## 3. Principles

- Every action that affects another person needs the user's approval, set in advance during onboarding.
- Emergency decisions come from fixed rules, not from the language model.
- The user can stop the assistant or revoke any permission at any time.
- The assistant does not control other apps on the phone.
- The assistant does not type into other apps. It types only in its own screens.

## 4. Out of Scope (Current Version)

- Clicking, scrolling, or navigating other apps on the user's behalf.
- Typing into other apps.
- Any feature that depends on Accessibility Service for automation.

These may be reconsidered only after written confirmation from Google Play policy.
