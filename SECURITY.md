# Security

## What TapDrop can and cannot do

- It answers NFC readers only as a tag. It cannot read tags, cards or phones.
- It sends an item when that item's share screen is open, or, with TapDrop
  closed, after the wearer picks TapDrop on the watch's own chooser. With
  one-tap sharing turned on, it sends the quick-tap item to any phone that
  touches the watch, with the display on or asleep. The wearer chooses that
  item and can set it to "Nothing".
- It accepts writes only while the Learn screen is open.
- It has no internet permission. Nothing you store in it leaves the watch
  except over NFC, to a phone held against it.
- A payment request moves no money. The payer approves or declines in their
  own UPI app.

## What to know before you store something

- Items are kept unencrypted in the app's private storage on the watch. That
  includes a Wi-Fi password.
- Anyone you tap receives the item in full. A Wi-Fi item hands over the
  password.
- The app is signed with a throwaway debug key. Install it only from a copy
  you trust, or build it yourself from `watch-app/`.
- Setup uses Android's Wireless debugging. While it is on, a paired computer on
  the same network can control the watch. Turn it off when setup is done.

## Reporting a problem

Please do not open a public issue for a security problem. Use GitHub's
"Report a vulnerability" button on this repo's Security tab, so it can be fixed
before it is public.
