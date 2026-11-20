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
| Learned tag | Whatever an NFC tag app wrote to the watch | Written by NFC Tools; read-back not tested |

## Using it

A fresh install has no items. The home screen lists what you can add: contact,
Wi-Fi, pay me, location, link. Add one and it becomes a row in your list.

Tap a row to open its share screen, then hold the watch to a phone. Tap **Edit**
on that screen to change or delete the item. The last row of the list adds
another. On a payment request, the bezel sets the amount in steps of 10 rupees.

In the Wi-Fi form, "Use current Wi-Fi" fills in the network the watch is
connected to. The password is always typed once, because Android gives no app
access to saved Wi-Fi passwords.

The watch answers a phone only while a share screen is open, and only with
that item.

### Learn from phone

Add > **Learn from phone** turns the watch into an empty, writable tag. Open
any NFC tag-writing app on a phone, such as NFC Tools, build a record, press
Write and hold the phone to the watch. TapDrop saves what it receives as a new
item and shares it back unchanged. It names the item from its contents: a link's
address, a contact's name, a Wi-Fi network's name.

This is the quick way to load a Wi-Fi password or anything TapDrop has no form
for. The watch accepts writes only while the Learn screen is open.

It copies the data held on ordinary NFC stickers. It cannot copy metro, access
or bank cards, which rely on encryption and a fixed chip ID.

## Setting it up with Claude

`skill/tapdrop-watch/` is a Claude skill that walks through the whole setup on
someone else's machine: installing adb, pairing the watch, installing the app
and filling in their details. It bundles a prebuilt APK, so no JDK or Android
SDK is needed. `build.ps1` refreshes that APK on every build.

## Limits

- Samsung Health registers the same NFC tag ID, and only the foreground app
  can claim priority. So the share screen has to be on screen. With TapDrop
  closed, a tap opens Samsung's "Select an app to use" screen. Sharing from
  the watch face or with the display asleep was tested and does not work.
- Items are typed on the watch keyboard, or set from a computer over adb.
  There is no phone companion app yet.
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
    the share screen, `EditActivity.java` the add and edit forms,
    `LearnActivity.java` the Learn screen.
  - `Ui.java` holds colours, type and shared view pieces. `Icons.java` draws
    the icons in code, so there are no image files.
  - `build.ps1` builds and installs without Gradle.
- `web/pay.html` — static page that hands a UPI request to the payer's app.
- `skill/tapdrop-watch/` — the setup skill: instructions, `scripts/watch.py`,
  a troubleshooting guide, and the bundled APK and pay page.
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
