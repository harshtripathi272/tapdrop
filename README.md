# TapDrop

A Wear OS app that turns a Galaxy Watch into a programmable NFC tag. Pick
something on the watch, tap your wrist on a phone, and the phone gets it. The
receiving phone needs no app.

## Status

Prototype. Tested on a Galaxy Watch8 Classic (SM-L500, Android 17) against a
Galaxy S24 Ultra (Android 17). Not tested on an iPhone.

| Item | What the phone receives | Result on the S24 Ultra |
|---|---|---|
| Contact | vCard | Save-contact prompt |
| Wi-Fi | Wi-Fi Simple Configuration record | Join-network prompt |
| Payment request | Link to `web/pay.html` carrying a UPI request | UPI app chooser |
| Link | URL | Opens |
| Location | Google Maps link to the watch's current position | Not tested yet |

## Using it

The home screen is a list of your items. Tap one to open its share screen,
then hold the watch to a phone. Hold a row to edit or delete it. The last row
adds a new item. On a payment request, the bezel sets the amount in steps of
10 rupees.

The watch answers a phone only while a share screen is open, and only with
that item.

## Limits

- Samsung Health registers the same NFC tag ID, and only the foreground app
  can claim priority. So the share screen has to be on screen.
- Items are typed on the watch keyboard. There is no phone companion app yet.
- `web/pay.html` has to be reachable by the payer's phone. It is not hosted
  anywhere yet.
- Wi-Fi passwords are stored unencrypted in the app's private storage on the
  watch.

## Layout

- `tapdrop-spike/` — the watch app. Plain Java, framework classes only.
  - `NdefTagService.java` answers NFC readers as a Type 4 tag.
  - `Kinds.java` defines each item type: fields, labels, NDEF record.
  - `Ndef.java` builds the NDEF records.
  - `Item.java`, `Store.java` hold the user's items.
  - `MainActivity.java` is the list, `ShareActivity.java` and `CardView.java`
    the share screen, `EditActivity.java` the add and edit forms.
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

## Seeding an item over adb

For testing. Each command creates or replaces one item and opens its share
screen.

    adb shell am start -n dev.tapdrop.spike/.MainActivity --es type contact --es name "Asha Rao" --es phone "+15550100"
    adb shell am start -n dev.tapdrop.spike/.MainActivity --es type wifi --es ssid "Guest" --es pass "..."
    adb shell am start -n dev.tapdrop.spike/.MainActivity --es type upi --es upi_id "name@bank" --es upi_name "Asha Rao" --es amount 250
    adb shell am start -n dev.tapdrop.spike/.MainActivity --es type link --es label "Site" --es url "https://example.com"
    adb shell am start -n dev.tapdrop.spike/.MainActivity --es type location

Set where payment requests are sent, once:

    adb shell am start -n dev.tapdrop.spike/.MainActivity --es pay_page "https://.../pay.html"

A payment request moves no money. The payer sees the name and UPI ID in their
own app and approves or declines there.
