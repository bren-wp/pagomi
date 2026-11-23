#!/usr/bin/env python3
"""Save a genuine Android screen from a running, foreground pagomi. instance."""
from pathlib import Path
import argparse
import subprocess

SCREENS = ("splash", "onboarding", "home", "scanner", "review-scan", "files", "tools", "pdf-editor", "settings")

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("screen", choices=SCREENS)
    parser.add_argument("--serial")
    args = parser.parse_args()
    adb = ["adb"] + (["-s", args.serial] if args.serial else [])
    activity = subprocess.check_output(adb + ["shell", "dumpsys", "activity", "activities"], text=True)
    resumed = [line for line in activity.splitlines() if "ResumedActivity" in line or "resumedActivity" in line]
    if not any("com.brendigo.pagomi" in line for line in resumed):
        raise SystemExit("Open pagomi. and navigate to the requested screen before capturing.")
    png = subprocess.check_output(adb + ["exec-out", "screencap", "-p"])
    if not png.startswith(b"\x89PNG\r\n\x1a\n"):
        raise SystemExit("Android screenshot was not a PNG.")
    root = Path(__file__).resolve().parents[1]
    out = root / "brending" / "screenshots" / (args.screen + ".png")
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(png)
    print(out)

if __name__ == "__main__":
    main()
