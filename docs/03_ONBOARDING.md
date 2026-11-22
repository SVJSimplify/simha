# Guardian Angel Assistant: Onboarding Spec

## 1. Goals

- A senior citizen can finish setup alone.
- A family member can finish setup with them in under 10 minutes.
- Every permission is explained in plain words and can be changed later.

## 2. Design Rules

- Large text, high contrast, large buttons.
- Voice guidance in the user's language. Every screen can be read aloud.
- One question per screen. Plain words, no jargon.
- Maximum 6 steps. Each optional step can be skipped and finished later.
- Caregiver mode: a family member finishes setup on the person's phone, with the person present and consenting.
- No step requires typing a long password. Use a PIN or a one-tap confirmation.

## 3. Steps

1. **Language and name**
   - Choose a language.
   - Enter the name the assistant uses.

2. **Emergency contacts**
   - Pick contacts from the phone's contact list through the system picker.
   - Mark each as family, neighbor, or doctor.

3. **Doctor and hospital (optional)**
   - Pick the family doctor and a nearby hospital.

4. **Medical basics (optional)**
   - Conditions, medicines, and allergies.
   - Can be skipped and filled in later.

5. **Permissions, one at a time**
   - Each screen shows the permission, why it is needed, and a "Why?" button.
   - Usage access: "So the assistant knows you are active."
   - Microphone: "So you can talk to the assistant."
   - Location: "Only shared when you ask for help or in an alert."
   - Notifications: "So you get check-in messages."
   - The person may decline any permission. The app explains what changes.

6. **Approvals**
   - "May the assistant call your family for you?" Yes or no.
   - "May the assistant call a hospital for you?" Yes or no.
   - "May the assistant record your messages?" Yes or no.
   - Every answer can be changed in Settings at any time.

7. **Practice check-in**
   - The assistant runs a test check-in.
   - The person answers "I am fine" by voice or by tapping a large button.
   - The assistant confirms the setup is complete.

## 4. Done When

- The person hears the check-in and answers it.
- The person knows how to stop the assistant and change settings.
- Each approval is recorded in the audit log.

## 5. Accessibility Checks

- Test with a user over 65 who has not used a smartphone before.
- Test with reduced vision and hearing aids.
- Test in a noisy room.
- Test with one hand.
