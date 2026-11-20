# How TapDrop works

Notes on the parts that are not obvious from reading the code. Everything here
was measured on a Galaxy Watch8 Classic and a Galaxy S24 Ultra, both on
Android 17.

## The watch pretends to be an NFC tag

Android's host card emulation lets an app answer an NFC reader in software.
TapDrop uses it to emulate an NFC Forum Type 4 tag, the same thing as an NFC
sticker. A phone reads it the way it reads any sticker, which is why the phone
needs no app.

A read is five commands and takes about 100 ms:

1. SELECT the tag application, ID `D2760000850101`
2. SELECT file `E103`, the capability container
3. READ the container: sizes, and whether the tag is writable
4. SELECT file `E104`, the data
5. READ the length, then READ the message

The message is built at the moment of the tap, from the item on screen. That is
why the location item can carry where you are right now.

Code: `watch-app/src/dev/tapdrop/NdefTagService.java`.

## What each item sends

| Item | NDEF record | What an Android phone does |
|---|---|---|
| Contact | MIME `text/vcard` | Offers to save the contact |
| Wi-Fi | MIME `application/vnd.wfa.wsc` | Offers to join the network |
| Pay me | Web link to the pay page | Opens the page, then the UPI app chooser |
| Location | Web link to Google Maps | Opens Maps |
| Link | URI | Opens it |
| Learned tag | Whatever was written | Depends on the record |

iPhones act on web links from a tag and little else. Links and location should
work. Contact, Wi-Fi and learned records of other types are untested there.

## Why a payment request goes through a web page

Phones hand a tag's content to apps with a dedicated NFC action, not the one
used for a link click. On the test phone, nine apps accepted a `upi://pay` link
from a click, including GPay, PhonePe, Paytm and BHIM. None accepted it from a
tag. Android then fell back to apps that react to any tag at all, so the payer
saw a list of card readers buried in shopping and bank apps.

So the tag carries a web link to `web/pay.html`, with the request after the
`#`. The page opens in the browser and forwards to `upi://pay?...`. That is a
link click, so every UPI app is offered.

The part of a link after `#` is never sent to a server. The page stores
nothing and receives nothing. It checks the UPI ID and amount, and tells the
payer it is only a request.

## Samsung Health claims the same tag ID

The tag ID above is fixed by the standard. It is the one every phone looks for,
so TapDrop cannot pick another. Samsung Health on the watch registers it too.

Android settles the clash like this:

- An app that is **on screen** can claim priority. While a TapDrop item is
  open, TapDrop answers.
- The moment TapDrop leaves the screen, Android drops that priority by itself.
  A tap then opens Samsung's "Select an app to use" screen. The wearer picks
  TapDrop and the phone taps again.

So with TapDrop closed, sharing takes two taps, and TapDrop sends the wearer's
**quick-tap item**. The choice on that screen doubles as consent: a passing
phone gets nothing unless the wearer picks TapDrop.

## One-tap sharing

Android has a call for this clash,
`CardEmulation.setServiceEnabledForCategoryOther`, and an ordinary app may use
it on another app's NFC service. TapDrop's One-tap sharing screen uses it to
switch off Samsung Health's tag service. TapDrop is then the only app answering
that ID, the chooser never appears, and one tap works from the watch face.

It is off by default and only the wearer's own press changes it, because it has
two costs. Samsung Health's NFC tag feature stops working. And the chooser was
the confirmation step, so afterwards any phone that touches the watch gets the
quick-tap item unasked, with the display on or asleep.

The wearer chooses the quick-tap item on the Quick tap screen, and "Nothing" is
one of the choices. The tag service is declared with
`requireDeviceScreenOn="false"`, which is what lets a tap through while the
display sleeps.

Code: `watch-app/src/dev/tapdrop/OneTapActivity.java`.

## Learn mode: the watch can be written to

The tag standard covers writing as well as reading. While the Learn screen is
open, the watch presents itself as an empty writable tag. A tag-writing app on
a phone then does what it would do to a blank sticker:

1. Sets the length to 0
2. Writes the message
3. Sets the real length

TapDrop saves the message when the third step arrives, and shares it back byte
for byte. It names the item from its first record: a link's address, a
contact's name, a Wi-Fi network's name.

Writes are refused at every other time, so nobody can overwrite an item by
brushing past the watch. Messages can be up to about 2 KB.

"Is the Learn screen open" is held in memory, never saved. An early build saved
it, and when the app was killed with that screen open the watch went on
presenting an empty writable tag to every phone. State that dies with the
process cannot be left behind.

This copies NDEF data, which is what ordinary stickers hold. Transit, access
and payment cards do not work that way. They rely on encryption and a fixed
chip ID, and cannot be copied like this.

## Built without Gradle

`watch-app/build.ps1` calls aapt2, javac, d8, zipalign and apksigner directly.
The app uses only Android framework classes, so there are no dependencies to
resolve, and a build takes under a minute. Icons are drawn in code and the
launcher icon is a vector, so the APK is about 40 KB.
