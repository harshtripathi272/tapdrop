#!/usr/bin/env python3
"""Drive a Wear OS watch over adb for TapDrop. Standard library only.

    watch.py status                 adb, connected devices, wireless-debugging services
    watch.py pair-target            wait for a pairing screen, print the `adb pair` command
    watch.py connect                find paired devices on the network and connect
    watch.py install [apk]          install TapDrop on the watch
    watch.py set KIND key=value...  create or replace an item (contact, wifi, upi, link, location)
    watch.py pay-page URL           where UPI requests are sent
    watch.py open                   wake the watch and open TapDrop
    watch.py foreground             exit 0 if TapDrop is on screen, 1 otherwise
    watch.py log [--clear]          TapDrop's NFC log, to confirm a phone read an item
    watch.py uninstall              remove TapDrop from the watch
"""
import argparse
import os
import re
import shlex
import shutil
import subprocess
import sys
import time
from pathlib import Path

PACKAGE = "dev.tapdrop.spike"
MAIN = PACKAGE + "/.MainActivity"
KINDS = ("contact", "wifi", "upi", "link", "location")
FIELDS = {
    "contact": ("name", "phone", "email"),
    "wifi": ("ssid", "pass"),
    "upi": ("upi_id", "upi_name", "amount"),
    "link": ("label", "url"),
    "location": (),
}
SECRET = {"pass"}
DEFAULT_APK = Path(__file__).resolve().parent.parent / "assets" / "tapdrop.apk"


def find_adb():
    found = shutil.which("adb")
    if found:
        return found
    exe = "adb.exe" if os.name == "nt" else "adb"
    roots = [os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT")]
    if os.name == "nt":
        roots.append(os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk"))
    roots += [str(Path.home() / "Library" / "Android" / "sdk"), str(Path.home() / "Android" / "Sdk")]
    for root in roots:
        if root:
            candidate = Path(root) / "platform-tools" / exe
            if candidate.is_file():
                return str(candidate)
    return None


ADB = find_adb()


def adb(*args, timeout=60):
    """Runs adb and returns (combined output, exit code)."""
    try:
        done = subprocess.run([ADB, *args], capture_output=True, text=True, timeout=timeout,
                              encoding="utf-8", errors="replace")
    except subprocess.TimeoutExpired:
        return "timed out", 1
    return (done.stdout + done.stderr).strip(), done.returncode


def shell(serial, command, timeout=60):
    return adb("-s", serial, "shell", command, timeout=timeout)


def devices():
    """Connected devices as dicts: serial, kind ('watch' or 'phone'), model."""
    out, _ = adb("devices")
    found = []
    for line in out.splitlines()[1:]:
        parts = line.split("\t")
        if len(parts) != 2 or parts[1].strip() != "device":
            continue
        serial = parts[0].strip()
        traits, _ = shell(serial, "getprop ro.build.characteristics")
        model, _ = shell(serial, "getprop ro.product.model")
        found.append({"serial": serial, "kind": "watch" if "watch" in traits else "phone", "model": model})
    # adb lists a paired device twice: once by ip:port and once by its mDNS name. Keep the ip:port one.
    by_address = [d for d in found if re.match(r"^\d+\.\d+\.\d+\.\d+:\d+$", d["serial"])]
    models = {d["model"] for d in by_address}
    return by_address + [d for d in found if d not in by_address and d["model"] not in models]


def mdns():
    """Wireless-debugging services on the network as (name, type, ip:port)."""
    out, _ = adb("mdns", "services")
    services = []
    for line in out.splitlines():
        parts = line.split()
        if len(parts) >= 3 and parts[1].startswith("_adb"):
            services.append((parts[0], parts[1], parts[-1]))
    return services


def watch():
    for device in devices():
        if device["kind"] == "watch":
            return device["serial"]
    sys.exit("No watch is connected over adb. Run `watch.py connect`, or see references/troubleshooting.md.")


def foreground(serial):
    out, _ = shell(serial, "dumpsys activity activities | grep topResumedActivity")
    return PACKAGE + "/" in out


# ---- commands ----------------------------------------------------------------

def cmd_status(_):
    version, _code = adb("version")
    print("adb:", ADB)
    print(version.splitlines()[1] if len(version.splitlines()) > 1 else version)
    found = devices()
    print("\nConnected devices:" if found else "\nNo devices connected.")
    for device in found:
        print(f"  {device['kind']:5}  {device['model']:12}  {device['serial']}")
    services = mdns()
    print("\nWireless debugging seen on the network:" if services else "\nNo wireless debugging seen on the network.")
    for name, kind, address in services:
        print(f"  {kind:24} {address:24} {name}")


def cmd_pair_target(args):
    adb("start-server")
    connected = {d["serial"].split(":")[0] for d in devices()}
    print(f"Waiting up to {args.wait}s for a pairing screen "
          "(Developer options > Wireless debugging > Pair new device)...")
    deadline = time.time() + args.wait
    while time.time() < deadline:
        for _name, kind, address in mdns():
            # Skip devices that are already connected. With two pairing screens open,
            # a code typed for one device would otherwise be sent to the other.
            if "pairing" in kind and address.split(":")[0] not in connected:
                print(f"\nFound a pairing screen at {address}")
                print("Have the user run this in their own terminal and type the 6 digit code:")
                print(f"\n    {ADB} pair {address}\n")
                return
        time.sleep(1)
    sys.exit("No pairing screen appeared. Is the device on the same network, with the pairing screen open?")


def cmd_connect(args):
    adb("start-server")
    deadline = time.time() + args.wait
    tried = set()
    while time.time() < deadline:
        for _name, kind, address in mdns():
            if "connect" in kind and address not in tried:
                tried.add(address)
                out, _ = adb("connect", address)
                print(out)
        if any(d["kind"] == "watch" for d in devices()):
            break
        time.sleep(1)
    cmd_status(args)
    if not any(d["kind"] == "watch" for d in devices()):
        print("\nNo watch connected. If the watch shows an IP and port under Wireless debugging, "
              "try `adb connect <ip>:<port>` with those values. Discovery sometimes misses a device "
              "that still accepts a direct connection.")


def cmd_install(args):
    apk = Path(args.apk) if args.apk else DEFAULT_APK
    if not apk.is_file():
        sys.exit(f"APK not found: {apk}")
    serial = watch()
    out, code = adb("-s", serial, "install", "-r", str(apk), timeout=180)
    print(out)
    if code != 0:
        sys.exit(1)


def cmd_set(args):
    values = {}
    for pair in args.fields:
        if "=" not in pair:
            sys.exit(f"Expected key=value, got: {pair}")
        key, value = pair.split("=", 1)
        if key not in FIELDS[args.kind]:
            sys.exit(f"{args.kind} has no field '{key}'. Fields: {', '.join(FIELDS[args.kind]) or 'none'}")
        values[key] = value
    serial = watch()
    command = f"am start -n {MAIN} --es type {args.kind}"
    if args.id:
        command += " --es id " + shlex.quote(args.id)
    if not args.open:
        command += " --ez quiet true"
    for key, value in values.items():
        command += f" --es {key} {shlex.quote(value)}"
    shell(serial, "input keyevent KEYCODE_WAKEUP")
    out, code = shell(serial, command)
    if code != 0 or "Error" in out:
        sys.exit(out)
    shown = ", ".join(f"{k}={'(hidden)' if k in SECRET else v}" for k, v in values.items())
    print(f"Set {args.kind}" + (f" [{args.id}]" if args.id else "") + (": " + shown if shown else ""))


def cmd_pay_page(args):
    serial = watch()
    shell(serial, "input keyevent KEYCODE_WAKEUP")
    out, _ = shell(serial, f"am start -n {MAIN} --es pay_page {shlex.quote(args.url)}")
    print("Pay page set." if "Error" not in out else out)


def cmd_open(_):
    serial = watch()
    shell(serial, "input keyevent KEYCODE_WAKEUP")
    shell(serial, f"am start -n {MAIN}")
    time.sleep(1.5)
    print("TapDrop is on screen." if foreground(serial) else "TapDrop did not come to the front. Is the watch unlocked?")


def cmd_foreground(_):
    on_screen = foreground(watch())
    print("TapDrop is on screen." if on_screen else "TapDrop is not on screen.")
    sys.exit(0 if on_screen else 1)


def cmd_log(args):
    serial = watch()
    if args.clear:
        adb("-s", serial, "logcat", "-c")
        print("Log cleared.")
        return
    out, _ = adb("-s", serial, "logcat", "-d", "-s", "TapDrop:D")
    lines = [l for l in out.splitlines() if re.search(r"sharing|serving|READ COMPLETE|not answering|nothing to send", l)]
    print("\n".join(lines[-20:]) if lines else "No NFC activity logged yet.")


def cmd_uninstall(_):
    out, _ = adb("-s", watch(), "uninstall", PACKAGE)
    print(out)


def main():
    parser = argparse.ArgumentParser(description="Drive a Wear OS watch over adb for TapDrop.")
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("status").set_defaults(run=cmd_status)

    p = sub.add_parser("pair-target")
    p.add_argument("--wait", type=int, default=120)
    p.set_defaults(run=cmd_pair_target)

    p = sub.add_parser("connect")
    p.add_argument("--wait", type=int, default=30)
    p.set_defaults(run=cmd_connect)

    p = sub.add_parser("install")
    p.add_argument("apk", nargs="?")
    p.set_defaults(run=cmd_install)

    p = sub.add_parser("set")
    p.add_argument("kind", choices=KINDS)
    p.add_argument("fields", nargs="*", metavar="key=value")
    p.add_argument("--id", help="keep several items of one kind, e.g. --id instagram")
    p.add_argument("--open", action="store_true", help="open the item's share screen afterwards")
    p.set_defaults(run=cmd_set)

    p = sub.add_parser("pay-page")
    p.add_argument("url")
    p.set_defaults(run=cmd_pay_page)

    sub.add_parser("open").set_defaults(run=cmd_open)
    sub.add_parser("foreground").set_defaults(run=cmd_foreground)

    p = sub.add_parser("log")
    p.add_argument("--clear", action="store_true")
    p.set_defaults(run=cmd_log)

    sub.add_parser("uninstall").set_defaults(run=cmd_uninstall)

    args = parser.parse_args()
    if ADB is None:
        sys.exit("adb was not found. Install Android platform-tools first (see SKILL.md, step 1).")
    args.run(args)


if __name__ == "__main__":
    main()
