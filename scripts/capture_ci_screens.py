#!/usr/bin/env python3
"""Capture actual pagomi screens on a disposable CI emulator, not concept art."""
import json
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

APP = "com.brendigo.pagomi.test"
ACTIVITY = APP + "/com.brendigo.pagomi.MainActivity"
OUT = Path("build/visual-evidence")

def adb(*args, text=True):
    return subprocess.check_output(["adb", *args], text=text, timeout=75)

def hierarchy():
    adb("shell", "uiautomator", "dump", "/sdcard/pagomi-ui.xml")
    return ET.fromstring(adb("shell", "cat", "/sdcard/pagomi-ui.xml"))

def wait_for(predicate, description):
    end = time.monotonic() + 60
    while time.monotonic() < end:
        try:
            root = hierarchy()
            if predicate(root):
                return root
        except (subprocess.SubprocessError, ET.ParseError):
            pass
        time.sleep(2)
    raise RuntimeError("Screen not confirmed: " + description)

def nodes(root, label):
    return [node for node in root.iter("node") if node.get("text") == label or node.get("content-desc") == label]

def bounds(node):
    return [int(x) for x in re.findall(r"\d+", node.get("bounds", ""))]

def capture(name, expected):
    wait_for(lambda r: any(nodes(r, label) for label in expected), name)
    time.sleep(1)
    png = adb("exec-out", "screencap", "-p", text=False)
    if not png.startswith(b"\x89PNG\r\n\x1a\n"):
        raise RuntimeError("Invalid screen capture: " + name)
    (OUT / (name + ".png")).write_bytes(png)

def navigate(label):
    root = wait_for(lambda r: bool(nodes(r, label)), label)
    choices = [bounds(n) for n in nodes(root, label)]
    choices = [b for b in choices if len(b) == 4 and b[2] > b[0] and b[3] > b[1]]
    if not choices:
        raise RuntimeError("No visible bounds for " + label)
    x1, y1, x2, y2 = max(choices, key=lambda b: b[3])
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))

def start():
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)

def main():
    if adb("shell", "getprop", "ro.kernel.qemu").strip() != "1":
        raise SystemExit("Only a disposable emulator may be reset by this script.")
    OUT.mkdir(parents=True, exist_ok=True)
    apk = Path("app/build/outputs/apk/debug/app-debug.apk")
    adb("install", "-r", str(apk))
    adb("shell", "pm", "clear", APP)
    adb("shell", "cmd", "uimode", "night", "no")
    start()
    capture("onboarding", ["Scan smarter"])
    # Set consent and theme exclusively in the isolated debug fixture, after capturing first launch.
    adb("shell", "am", "force-stop", APP)
    adb("shell", "run-as " + APP + " sh -c 'mkdir -p files; printf \"%s\" \"{\\\"version\\\":2,\\\"at\\\":0}\" > files/consent.json; printf light > files/theme.txt'")
    start()
    capture("home", ["Scan Document"])
    navigate("Files")
    capture("files", ["My Files"])
    navigate("Tools")
    capture("tools", ["Powerful PDF tools"])
    navigate("Settings")
    capture("settings", ["Appearance", "Manage your preferences, storage and privacy."])
    (OUT / "capture-metadata.json").write_text(json.dumps({
        "commit": os.getenv("GITHUB_SHA", ""),
        "api": adb("shell", "getprop", "ro.build.version.sdk").strip(),
        "display": adb("shell", "wm", "size").strip(),
        "density": adb("shell", "wm", "density").strip(),
        "theme": "light",
        "fixture": "isolated debug install; no real user files",
        "screens": ["onboarding", "home", "files", "tools", "settings"],
        "visual_parity_verified": False
    }, indent=2) + "\n")

if __name__ == "__main__":
    main()
