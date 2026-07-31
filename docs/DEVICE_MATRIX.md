# Device and configuration matrix

The min SDK is 26 and target/compile SDK is 36. The following matrix covers API
behavior, screen width, navigation, text scale, theme, and manufacturer
background restrictions.

| Priority | Device profile | API | Size / density | Why |
|---|---|---:|---|---|
| P0 | Pixel emulator | 26 | compact phone | Minimum supported Android |
| P0 | Pixel emulator | 33 | compact phone | Notification runtime permission |
| P0 | Pixel emulator | 36 | current phone | Target SDK behavior |
| P0 | Small phone emulator | 36 | 320–360 dp width | Bottom labels and forms |
| P0 | Pixel Tablet emulator | 36 | expanded width | Responsive spacing and lists |
| P1 | Samsung physical phone | 33+ | common phone | One UI and background work |
| P1 | Xiaomi/Redmi physical phone | 33+ | common phone | Aggressive battery management |
| P1 | Foldable emulator | 36 | folded/unfolded | Activity and layout continuity |
| P1 | Any phone | 36 | font scale 1.3× and 2.0× | Large text/accessibility |

Run each P0 profile in light and dark theme, portrait, English locale, offline
and reconnect, and with notification permission granted and denied.

Additional configuration cases:

- 12/24-hour system clock (app appointment source uses explicit 24-hour text).
- Right-to-left system locale; current product content is LTR-prepared and must
  be reviewed before claiming RTL localization.
- Display size default and largest.
- Battery saver and Doze.
- App process killed, device rebooted, and session restored.
- Customer logout followed by Barber login on the same device.
- Slow network, airplane mode, and network switching.

