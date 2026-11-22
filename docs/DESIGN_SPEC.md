# Simha: Design Spec (Soft Sand)

## 1. Theme

Option C, Soft Sand. Colors and contrast are in `Color.kt`.

| Role | Hex | Use | Contrast |
|---|---|---|---|
| Background | #F4EDE1 | Page | |
| Primary text | #2E4057 | Body, headings | 8.2:1 on background |
| Secondary text | #4A5A6E | Supporting text | 6.4:1 on background |
| Primary button | #2E4057 fill, #F4EDE1 text | Main actions | 8.2:1 |
| Emergency button | #9C3A22 fill, #FFFFFF text | Emergency only | 7.5:1 |
| Accent (decor) | #E07A5F | Icons, dividers. Never text. | Not for text |
| Success | #2F6B4F | Check-in confirmed | 6.0:1 on background |

**Change from the palette:** Coral #E07A5F is too light for text (about 3:1). The emergency button uses a darker red-brown, #9C3A22, with white text.

## 2. Dynamic Layout

Layout reacts to two inputs:

1. **Width class** (Compact, Medium, Expanded), from `WindowSizeClass`.
2. **System font scale**, from `LocalConfiguration.fontScale`.

| Width class | Typical phone | Home actions per row | Screen padding |
|---|---|---|---|
| Compact (under 600 dp) | 360 dp phones from 2018 onward | 1 | 16 dp |
| Medium (600 to 839 dp) | Small tablets, folded phones | 2 | 24 dp |
| Expanded (840 dp and up) | Tablets | 3 | 32 dp |

Font scale above 1.3x: buttons grow 16 dp taller, and touch targets go to 64 dp.

## 3. Type

| Style | Size | Weight | Use |
|---|---|---|---|
| Title | 28 sp | 500 | Screen titles |
| Body | 20 sp | 400 | Main text |
| Button | 22 sp | 500 | Buttons |
| Caption | 16 sp | 400 | Metadata only |

Minimum body text: 20 sp. Use `sp` everywhere so the system font setting works.

**Scripts:** Noto Sans for Latin, Noto Sans Devanagari, Noto Sans Telugu, Noto Sans Kannada, Noto Sans Tamil. Bundle them as font resources. Test each language at 2x font scale for line wrapping.

## 4. Touch and Motion

- Minimum touch target: 56 dp. 64 dp at large font scale.
- No swipe-only actions. Every swipe has a button.
- Motion: 200 ms fades only. No bouncing or parallax.
- Respect "remove animations" in system settings.

## 5. Dark Mode

Night colors are in `Color.kt`. Enable dark mode only from the user's system setting or from a night switch on the home screen. Default is light.

## 6. Checks Before Release

- Test at 360 x 740 dp, 360 x 780 dp, and 412 x 915 dp.
- Test each language at 1.0x and 2.0x font scale.
- Confirm every text pair meets 7:1 (body) or 4.5:1 (large text).
- Test with TalkBack on.
