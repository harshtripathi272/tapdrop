# Contributing

TapDrop has been tested on one watch and one phone. The most useful thing you
can send is what happened on yours.

## Tell us about your device

Open an issue with:

- Watch model and its Wear OS / Android version
- Phone model and Android or iOS version
- Which items worked, and what the phone showed for each
- For anything that failed, the output of
  `python skill/tapdrop-watch/scripts/watch.py log`

Reports from iPhones, Pixel Watches and setups from macOS or Linux are the ones
we have none of.

## Changing the code

- The app is in `watch-app/src/dev/tapdrop/`. It uses plain Java and Android
  framework classes only. Please keep it that way: no Gradle and no libraries
  is what keeps the build to one script and the APK tiny.
- [docs/HOW-IT-WORKS.md](docs/HOW-IT-WORKS.md) explains the NFC side.
- Build with `watch-app/build.ps1` on Windows. A build script for macOS and
  Linux would be a welcome contribution.
- There are no automated tests. Say in your pull request what you checked on a
  real watch, and what you could not check.
- If you change how setup works, update
  `skill/tapdrop-watch/SKILL.md` in the same pull request. Coding assistants
  follow that file, so it has to stay true.

## A new kind of item

Most ideas need no code: Learn mode takes anything an NFC tag app can write. A
built-in kind earns its place when it needs something the watch has to compute,
the way Location and Pay me do. Add it in `Kinds.java`.
