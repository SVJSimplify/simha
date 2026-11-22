# Simha: UI Design Document

**Product:** Simha (Guardian Angel Assistant)
**Platform:** Android, India first
**Theme:** Soft Sand (option C)
**Languages at launch:** English, Hindi, Telugu, Kannada, Tamil

## 1. Design Goals

- Easy to read for users over 65, including users with reduced vision.
- Works on Android phones from 2018 onward.
- Layout adapts to screen width and to the system font size.
- Every main action is reachable with one hand.
- Every screen can be read aloud.

## 2. Theme: Soft Sand

### 2.1 Colors

| Role | Color | Use |
|---|---|---|
| Page background | Soft sand (#F4EDE1) | Every screen |
| Card and dialog surface | Light cream (#FBF8F2) | Raised cards, dialogs |
| Alternate surface | Deep sand (#EAE0CF) | Grouped sections |
| Primary text | Slate blue (#2E4057) | Headings and body text |
| Secondary text | Muted slate (#4A5A6E) | Supporting text |
| Primary button | Slate blue fill, sand text | Main actions |
| Emergency button | Dark red-brown (#9C3A22) fill, white text | Emergency only |
| Success | Deep green (#2F6B4F) | Check-in confirmed |
| Decorative accent | Coral (#E07A5F) | Icons and dividers only, never text |
| Borders | Grey-blue (#8A96A3) | Inputs and dividers |

**Contrast rule:** Body text must reach 7:1 against its background. Large text must reach 4.5:1. Coral must not carry text, because it fails contrast on sand.

**Emergency color:** The emergency button uses a darker red-brown than the palette's coral, so white text reaches 7:1.

### 2.2 Dark Mode

Dark mode is for night use. It is off by default.

- Turn on from the system setting, or from a night switch on the home screen.
- Background: deep slate. Surface: lighter slate. Text: sand.
- Keep the emergency button color the same as in light mode.

## 3. Layout

### 3.1 Adapting to Screen Width

The layout changes at three width classes.

| Width class | Typical device | Home actions per row | Screen margin |
|---|---|---|---|
| Compact (under 600 dp) | Most phones, including 2018 models | 1 | 16 dp |
| Medium (600 to 839 dp) | Small tablets, folded phones | 2 | 24 dp |
| Expanded (840 dp and up) | Tablets | 3 | 32 dp |

Rules:
- Design for the Compact class first.
- Content stays in one column, centered, with a maximum width on wide screens.
- No content is hidden at any width class. Only the number of columns changes.

### 3.2 Adapting to System Font Size

- Use scalable text units everywhere, so the system font setting applies.
- When the system font is larger than 1.3 times normal, buttons grow taller and touch targets grow to 64 dp.
- No text may be clipped at 2 times normal font size. Let lines wrap or let the screen scroll.

### 3.3 Touch Targets

- Minimum touch target: 56 dp.
- Primary buttons: at least 72 dp tall on phones. Larger on tablets.
- Space between targets: at least 12 dp.
- Every swipe action has a visible button.

## 4. Typography

| Style | Size | Weight | Use |
|---|---|---|---|
| Title | 28 sp | Medium | Screen titles |
| Body | 20 sp | Regular | Main text |
| Button | 22 sp | Medium | Button labels |
| Caption | 16 sp | Regular | Metadata only, never instructions |

Rules:
- Body text is never smaller than 20 sp.
- Use sentence case.
- Keep line length to about 40 to 60 characters where possible.

### 4.1 Languages

- Latin script: Noto Sans.
- Devanagari (Hindi): Noto Sans Devanagari.
- Telugu: Noto Sans Telugu.
- Kannada: Noto Sans Kannada.
- Tamil: Noto Sans Tamil.

Bundle all fonts with the app. Do not rely on downloaded fonts.

Test each language at normal size and at 2 times font size. Indic scripts often need more height per line, so allow 30% more vertical space for those languages.

## 5. Screens

### 5.1 Home

- Status line at the top: "Simha is with you. Last check-in: 2 hours ago." Translated.
- One large primary button: "Talk to Simha."
- Three action buttons: Family, Doctor, Help.
- Help uses the emergency color and opens the emergency screen.

### 5.2 Check-in

- Full-screen prompt: "Are you okay?"
- Two large answers: "I am fine" and "I need help."
- The screen repeats the question aloud and waits for a response.

### 5.3 Emergency

- Steps appear one at a time.
- The dialer opens for emergency services. The user presses call.
- Recording starts only after a spoken notice.

### 5.4 Profile

- Conditions, medicines, allergies, and doctor.
- Each field is a large, labeled input.

### 5.5 Settings

- Each permission has a plain-language switch.
- Each approval (family calls, hospital calls, recording) has its own switch.

### 5.6 Caregiver Mode

Caregiver mode is part of the same app.

- A person registers and invites a caregiver.
- The caregiver accepts the invite and gets a separate home screen.
- The caregiver's home screen shows status, last check-in, call log, and alerts.
- The caregiver cannot see recordings unless the person approves.
- The person can remove a caregiver at any time.

## 6. Interaction

- Voice first. Every screen can be read aloud and answered by voice.
- No hidden menus. Every action is visible on its screen.
- No timed interactions. Screens wait for the user.
- Motion: short fades only, about 200 ms. No bouncing or sliding effects.
- Respect the system setting that removes animations.

## 7. Accessibility Checks Before Release

- Test every screen with the screen reader on.
- Test each language at normal and 2 times font size.
- Test at 360 by 740 dp, 360 by 780 dp, and 412 by 915 dp.
- Test contrast on every text pair.
- Test with a user over 65 who has not used a smartphone before.
- Test in a noisy room and with one hand.

## 8. Open Items

- Confirm the emergency color with a clinician or senior user test.
- Confirm translations for Hindi, Telugu, Kannada, and Tamil with native speakers.
- Confirm the caregiver invite method (code, link, or contact).
