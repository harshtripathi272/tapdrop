# Prompt

Clone this repo, open the folder in your coding assistant, and paste the text
below. It works with Claude Code, Codex, Gemini CLI and other assistants that
can run shell commands on your computer.

Fill in your watch model first.

```text
Set up TapDrop on my watch. My watch is: <model, e.g. Galaxy Watch8 Classic>.

You are in a clone of the TapDrop repo. Read AGENTS.md, then follow
skill/tapdrop-watch/SKILL.md from the first step to the last. Use
skill/tapdrop-watch/scripts/watch.py for the adb work.

Go one step at a time and tell me what each step does before you do it.
Ask me before downloading anything. When something has to be done on the
watch itself, give me the exact taps and wait for me.

When the app is installed, ask me what I want to share (contact, Wi-Fi,
a UPI payment request, my location, links) and set those up. Then have me
test a tap with my phone and check the watch's log to confirm it worked.
```

## What to expect

About ten minutes. The assistant will:

1. Check that your watch can do this.
2. Install Google's `adb` tool if you don't have it.
3. Walk you through turning on debugging on the watch and pairing it.
4. Install TapDrop.
5. Fill in what you want to share.
6. Watch a test tap with you.

You will be asked to type a 6 digit pairing code from the watch into your own
terminal. That code is yours to enter, not the assistant's.
