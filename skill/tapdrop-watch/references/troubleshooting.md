# Troubleshooting

Problems that came up while building and testing TapDrop, with what fixed them.
Each one cost real time the first time round.

## Contents

- Connecting: watch not found, pairing fails, device goes offline
- Driving the watch: inputs land in the wrong place, stale screen dumps
- NFC: phone shows nothing, wrong app list, no answer
- Environment: tools you installed are missing

## Connecting

**`adb mdns services` shows nothing, or not the watch.**
Discovery is unreliable. If the watch's Wireless debugging screen shows an IP
and port, connect straight to them with `adb connect <ip>:<port>`. A watch that
has gone missing from discovery often still accepts a connection on its last
port.

**The watch never appears on the network at all.**
Either Wireless debugging is off (ADB debugging alone is not enough), or the
network isolates clients. Campus, hotel and guest Wi-Fi commonly do. A hotspot
from the computer is the dependable fix: join the watch to it.

**Pairing fails with "protocol fault".**
The code went to the wrong device. This happens when two devices have pairing
screens open and the tool picks the first one it sees. Close the other pairing
screen, or pair by IP. `watch.py pair-target` skips devices that are already
connected for this reason. Codes are single use, so get a fresh one.

**The pairing port does not work for connecting.**
They are different ports. The pairing port is on the "Pair new device" screen.
The connect port is on the main Wireless debugging screen.

**The watch shows as `offline`, or commands fail after working a minute ago.**
Watches drop Wi-Fi when idle to save battery, more so when off the wrist. Wake
the watch, check it is still on Wi-Fi, then `adb disconnect <ip>:<port>` and
`adb connect <ip>:<port>` again. In Developer options, "Turn off automatic
Wi-Fi" keeps Wi-Fi up while you work.

**The port changed.**
The connect port changes every time Wireless debugging restarts. Read it from
`adb mdns services` or from the watch's screen. Pairing survives, so no new code
is needed.

**adb lists the watch twice.**
A paired device appears once as `ip:port` and once under its mDNS name. Use the
`ip:port` one with `-s`.

**Two devices are connected and a command hit the wrong one.**
Always pass `-s <serial>`. To tell which is the watch:
`adb -s <serial> shell getprop ro.build.characteristics` contains `watch`.

## Driving the watch

**A simulated tap or scroll changed something on the watch face.**
The watch returns to its watch face after a few idle seconds, and input events
go to whatever is on screen. Before sending any `input` command, wake the watch
(`input keyevent KEYCODE_WAKEUP`) and confirm TapDrop is in front with
`watch.py foreground`. Never send taps blind.

**`am start` brings back an old screen instead of the list.**
Starting the main activity brings the existing task forward with whatever was
on top. For a clean start: `am force-stop dev.tapdrop.spike`, then start it.

**`uiautomator dump` returns the previous screen's contents.**
When the dump fails it leaves the old file in place, and reading it makes it
look as if nothing changed. Delete the file before each dump.

**Scrolling with the bezel from adb.**
`adb shell input rotaryencoder scroll --axis SCROLL,-1` is one click clockwise.
Positive is anticlockwise.

**The share and edit screens cannot be started from adb.**
Only the main activity is exported. Seed items with `watch.py set`, or open
them by tapping in the list.

## NFC

**The phone does nothing when tapped.**
In order: is an item's share screen open on the watch, is NFC on on both
devices, is the phone unlocked, and is the phone's NFC antenna (upper back on
most phones) against the watch face. `watch.py log` shows whether the watch saw
a reader at all.

**The log says `not answering`.**
There was nothing to send: the list is empty, or the item is a location that
has no fix yet.

**The watch shows "Select an app to use" with Samsung Health and TapDrop tag.**
Expected when the phone taps while TapDrop is closed. Two apps claim the same
tag ID, and off screen neither has priority, so Samsung asks. Pick TapDrop tag,
then tap the phone again. The watch sends the quick-tap item. To skip this
screen, open an item first and tap once, or turn on One-tap sharing at the
bottom of TapDrop's home screen.

**One-tap sharing says the watch refused, or has no setting for it.**
It relies on an Android call, `setServiceEnabledForCategoryOther`, that newer
versions provide and makers may restrict. It worked on a Galaxy Watch8 Classic
on Android 17. Where it fails, the two-tap chooser path still works. To check
the result, look for Samsung Health in `adb shell dumpsys nfc`: its line should
read `Category: other(enabled: false)`.

**After picking TapDrop and tapping again, nothing happens.**
There is no quick-tap item to send. Either the list is empty, or the quick-tap
item is a location with no fix yet. `watch.py log` shows `nothing to share yet`
or `nothing to send yet for location`. An older build of the app stayed silent
whenever it was closed; reinstall the current one.

**Learn from phone: the tag app says the tag is read-only or not writable.**
The Learn screen was not open. The watch accepts writes only while it is
showing. Open TapDrop > Add > Learn from phone, then write.

**Learn from phone: the write seems to succeed but no item appears.**
Check `watch.py log`. `LEARNED` means it was saved. "not a valid NDEF message"
means the app wrote something TapDrop could not parse. Messages are limited to
about 2 KB.

**The share screen says "NFC priority refused".**
Another app holds NFC priority. Close other apps on the watch and reopen the
item.

**A payment request shows a list of bank or shopping apps.**
That is the bare `upi://` link problem. No UPI app accepts that link from a tag,
so Android falls back to apps that react to any tag. Set a pay page (SKILL.md,
step 7).

**A location item never becomes ready.**
It needs location permission, granted on the watch the first time the item is
opened, and then a fix. Indoors that can take a minute. A fix older than ten
minutes is not shared.

**"Use current Wi-Fi" fills nothing in.**
The watch is probably using the phone's connection over Bluetooth, not Wi-Fi.
It also needs location permission, because Android treats a network name as
location data.

## Environment

**A tool you installed is not there when the user runs it.**
Some agent environments run shell commands in a sandbox whose file writes never
reach the real disk. If adb "installed" fine for you but the user's terminal
cannot find it, redo the install from a terminal the user can see. Reads and
network calls from the sandbox are usually real, so this only bites on writes.

**Windows: d8 or apksigner fail when building from source.**
The `java` on PATH is often version 8. They need 11 or newer. Point `JAVA_HOME`
at a newer JDK for the build.
