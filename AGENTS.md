# Instructions for coding assistants

This file is for an AI coding assistant working in this repo: Claude Code,
Codex, Gemini CLI or any other. People read [README.md](README.md).

## What this repo is

TapDrop is a Wear OS app that makes a watch answer NFC readers as a tag. The
wearer opens an item on the watch and holds it to a phone, and the phone gets a
contact card, a Wi-Fi login, a UPI payment request, a location or a link. The
receiving phone needs no app.

## If the user wants TapDrop set up on their watch

This is the common request. Follow
[skill/tapdrop-watch/SKILL.md](skill/tapdrop-watch/SKILL.md) from start to
finish. It is the single source of truth for setup, and it covers adb,
pairing, installing, filling in the user's details and testing.

- Do the adb work with `python skill/tapdrop-watch/scripts/watch.py`. Run it
  with `status` first. Its commands are listed at the top of the file.
- Install the prebuilt app at `skill/tapdrop-watch/assets/tapdrop.apk`. Building
  from source is not needed for setup.
- When a step fails, read
  [skill/tapdrop-watch/references/troubleshooting.md](skill/tapdrop-watch/references/troubleshooting.md)
  before improvising. Most failures are already written up there.

## Rules that hold for every task here

These exist because the work touches someone's own watch and phone.

- **Ask before downloading.** Name the file, the source and the size.
- **The user types the pairing code.** Print the `adb pair` command and let
  them run it. Don't ask for the code in chat.
- **Look before you tap.** The watch drops to its watch face after a few idle
  seconds, and simulated input goes to whatever is on screen. Confirm TapDrop
  is in front (`watch.py foreground`) before any `input` command.
- **Don't turn on One-tap sharing for the user.** It switches off another
  app's NFC service and removes a confirmation step. Explain it and let them
  press the button on the watch.
- **Treat what they give you as private.** A Wi-Fi password or UPI ID goes to
  the watch and nowhere else. Don't repeat it back, log it, or put it in a
  commit.
- **Say what you could not check.** You can read the watch's log. You cannot
  see the other phone's screen unless the user tells you what it showed.
- **Offer to turn Wireless debugging off** when the setup is done.

## Repo map

| Path | What it is |
|---|---|
| `skill/tapdrop-watch/` | The setup procedure, `watch.py`, troubleshooting, and the bundled APK and pay page |
| `watch-app/` | The app's source. Plain Java, Android framework classes only, no Gradle |
| `watch-app/build.ps1` | Builds the APK on Windows and refreshes the copy in `skill/` |
| `web/pay.html` | Static page that hands a UPI request to the payer's app |
| `tools/` | PowerShell helpers for pairing and reconnecting on Windows |
| `docs/` | How it works, and the images the README uses |

## If the user wants to change the app

- Source is in `watch-app/src/dev/tapdrop/`. Start with `Kinds.java` (what can
  be shared) and `NdefTagService.java` (the NFC side).
  [docs/HOW-IT-WORKS.md](docs/HOW-IT-WORKS.md) explains the parts that are not
  obvious from the code.
- Build with `watch-app/build.ps1`. It needs Windows, a JDK 11 or newer, and it
  downloads Android build-tools 35 and platform 35 from Google on first run,
  checked against Google's checksums. There is no build script for macOS or
  Linux yet. Say so if the user is on one.
- A build installs to the first connected device that reports itself as a
  watch. Pass `-NoInstall` to build only.
- There are no automated tests. Verify on a real watch: read the log, take a
  screenshot, and have the user tap a phone. Don't report a change as working
  from a successful compile alone.
- The app ID is `dev.tapdrop`. Don't change it: an installed app cannot be
  updated across an ID change.

## What has and has not been tested

Tested: a Galaxy Watch8 Classic and a Galaxy S24 Ultra, both on Android 17,
set up from Windows.

Not tested: other watch models, iPhones as the receiving phone, setup from
macOS or Linux, and the location item. Tell the user when their situation
falls outside what was tested, and treat it as a first run.
