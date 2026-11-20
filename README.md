# TapDrop

A Wear OS app that turns a Galaxy Watch into a programmable NFC tag. Pick
something on the watch, tap your wrist on a phone, and the phone gets it. The
receiving phone needs no app.

## Status

Prototype. Tested on a Galaxy Watch8 Classic (SM-L500, Android 17) against a
Galaxy S24 Ultra (Android 17). Not tested on an iPhone.

| Slot | What the phone receives | Result on the S24 Ultra |
|---|---|---|
| Contact | vCard | Save-contact prompt |
| Wi-Fi | Wi-Fi Simple Configuration record | Join-network prompt |
| Pay me | Link to `web/pay.html` carrying a UPI request | UPI app chooser |
| Link | URL | Opens |

Turn the bezel to change slot. On Pay me, tap the screen and the bezel sets the
amount in steps of 10 rupees.

## Limits

- TapDrop must be open on the watch. Samsung Health registers the same NFC tag
  ID, and only the foreground app can claim priority.
- Slot contents are set from a computer over adb. There is no editing on the
  watch and no phone companion app yet.
- `web/pay.html` has to be reachable by the payer's phone. It is not hosted
  anywhere yet.

## Layout

- `tapdrop-spike/` — the watch app. Plain Java, framework classes only.
  - `NdefTagService.java` answers NFC readers as a Type 4 tag.
  - `Slots.java` defines the four slots and their stored fields.
  - `MainActivity.java`, `CardView.java` are the watch UI.
  - `build.ps1` builds and installs without Gradle.
- `web/pay.html` — static page that hands a UPI request to the payer's app.
- `reconnect-watch.ps1`, `pair-device.ps1` — adb over Wi-Fi helpers.

## Build

Windows, PowerShell. Needs JDK 17 at the path set in `build.ps1`. The script
downloads Android build-tools 35 and platform 35 from Google on first run and
checks them against Google's published checksums.

    powershell -NoProfile -ExecutionPolicy Bypass -File tapdrop-spike\build.ps1

It installs to the first connected device that reports itself as a watch. Pass
`-NoInstall` to build only.

## Setting a slot

    adb shell am start -n dev.tapdrop.spike/.MainActivity --es type contact --es name "Asha Rao" --es phone "+15550100"
    adb shell am start -n dev.tapdrop.spike/.MainActivity --es type wifi --es ssid "Guest" --es pass "..."
    adb shell am start -n dev.tapdrop.spike/.MainActivity --es type upi --es upi_id "name@bank" --es upi_name "Asha Rao" --es amount 250 --es pay_page "https://.../pay.html"
    adb shell am start -n dev.tapdrop.spike/.MainActivity --es type link --es url "https://example.com"

A pay-me request moves no money. The payer sees the name and UPI ID in their
own app and approves or declines there.
