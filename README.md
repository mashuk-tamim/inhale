# Inhale — Pause before you open

A minimalist Android app that helps you build a more mindful relationship with distracting apps. Instead of blocking them outright, Inhale adds a short, intentional pause with a guided breathing exercise before the app opens. Most of the time, that's all it takes to change your mind.

## How it works

1. **Pick your distracting apps** — Instagram, X, YouTube, whatever pulls you in.
2. **Try to open one** — Inhale appears instead, with a calming breathing circle (inhale 4s · hold 2s · exhale 4s) and a countdown.
3. **Make a choice** — once the countdown finishes you can *Open anyway*, or tap *Stay Mindful · See Insights* to walk away and see your progress.

Every pause is recorded: how many times you opened the app, how often you walked away, and how much time you saved.

## Features

- 🫁 **Guided 4-2-4 breathing exercise** on every pause screen
- ⏱️ **Configurable pause countdown** — global or per-app overrides
- 🔓 **Bypass windows** — after opening anyway, the app stays unpaused for a while (global or per-app)
- 📊 **Per-app insights** — screen time, opens, mindful exits, a 7-day usage chart and a Mindful Score ring
- 🌙 **Themes** — light, dark, AMOLED, or follow the system
- 🔒 **Fully private** — all data stays on your device. No accounts, no analytics, no internet permission needed for core functionality.

## Download

**[⬇️ Download the APK](https://github.com/mashuk-tamim/inhale/releases/latest/download/inhale-v1.0.0.apk)** — always the latest version, no extra clicks.

1. Open the downloaded file on your phone (allow "Install unknown apps" if asked).
2. Follow the onboarding to grant accessibility and (optional) usage access.

<details>
<summary><strong>Install blocked? Two one-time huddles on some devices (e.g. Oppo/ColorOS)</strong></summary>

**"App blocked to protect your device" (Play Protect)**
Google can't verify a personally signed APK, so Play Protect flags it by default. Either:

- Tap **More details → Install anyway (unsafe)** on the block screen, or
- If no option appears: **Play Store → profile icon → Play Protect → ⚙️ → turn off "Scan apps with Play Protect"**, install, then turn it back on.

The "(unsafe)" label only means the publisher isn't verified by Google — expected for any sideloaded APK.

**"Restricted setting" when granting Accessibility**
Android 13+ blocks sensitive permissions for sideloaded apps until you explicitly unlock them:

1. Long-press the Inhale icon → **App info** (or Settings → Apps → Inhale).
2. Tap the **⋮ menu** (top-right) → **Allow restricted settings**.
3. Return to the app and grant the permission — it now works.

</details>

Prefer to browse older versions? See the [Releases](https://github.com/mashuk-tamim/inhale/releases) page.

Requires Android 8.0 (API 26) or newer.

## Permissions

| Permission | Why |
|---|---|
| Accessibility service | Detects when a paused app is launched and shows the pause screen |
| Usage access (optional) | Powers the screen-time stats and weekly usage chart |

## Building from source

```bash
git clone https://github.com/mashuk-tamim/inhale.git
cd inhale
./gradlew assembleDebug
```

The APK is output to `app/build/outputs/apk/debug/app-debug.apk`.

## License

All rights reserved. Personal use is encouraged; please contact the author before redistributing.
