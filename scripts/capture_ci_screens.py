#!/usr/bin/env python3
"""Capture actual pagomi screens on a disposable CI emulator, not concept art."""
import json
import struct
import zlib
import os
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

APP = "com.brendigo.pagomi.test"
ACTIVITY = APP + "/com.brendigo.pagomi.MainActivity"
OUT = Path("build/visual-evidence")
TOOL_QUERIES = {
    "workflow": ("workflow", "PDF Workflow Builder"),
    "merge": ("merge pdfs", "Merge PDFs"),
    "alternate": ("alternate", "Alternate & Mix PDF"),
    "compress": ("compress pdf", "Compress PDF"),
    "split": ("split pdf", "Split PDF"),
    "splitText": ("split pdf by text", "Split PDF by Text"),
    "splitBookmarks": ("split pdf by bookmarks", "Split PDF by Bookmarks"),
    "splitHalf": ("split pdf in half", "Split PDF in Half"),
    "splitSize": ("split pdf by size", "Split PDF by Size"),
    "organize": ("organize pages", "Organize Pages"),
    "rotate": ("rotate pdf", "Rotate PDF"),
    "flip": ("flip pdf", "Flip PDF"),
    "nup": ("multiple pages", "Multiple Pages Per Sheet"),
    "crop": ("crop resize", "Crop & Resize"),
    "zip": ("pdf to zip", "PDF to ZIP"),
    "fillSign": ("fill sign", "Fill & Sign"),
    "watermark": ("add watermark", "Add Watermark"),
    "pageNumbers": ("add page numbers", "Add Page Numbers"),
    "redact": ("redact pdf", "Redact PDF"),
    "ocr": ("ocr scanned pdf", "OCR Scanned PDF"),
    "unlock": ("remove password", "Remove Password"),
    "metadata": ("edit pdf details", "Edit PDF Details"),
    "password": ("set password", "Set Password"),
}

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
    return [node for node in root.iter("node") if node.get("text") == label or node.get("content-desc") == label or node.get("resource-id") == label]

def bounds(node):
    return [int(x) for x in re.findall(r"\d+", node.get("bounds", ""))]

def capture(name, expected):
    root = wait_for(lambda r: all(nodes(r, label) for label in expected), name)
    for forbidden in ("Local", "Local processing · No account required", "Debug", "Developer mode"):
        if nodes(root, forbidden):
            raise RuntimeError("Unwanted label " + forbidden + " in " + name)
    time.sleep(1)
    png = adb("exec-out", "screencap", "-p", text=False)
    if not png.startswith(b"\x89PNG\r\n\x1a\n"):
        raise RuntimeError("Invalid screen capture: " + name)
    (OUT / (name + ".png")).write_bytes(png)

def visible_choices(root, label, enabled=False):
    choices = []
    for node in nodes(root, label):
        if enabled and node.get("enabled") != "true":
            continue
        b = bounds(node)
        if len(b) == 4 and b[2] > b[0] and b[3] > b[1]:
            choices.append(b)
    return choices

def tap_bounds(choices):
    x1, y1, x2, y2 = max(choices, key=lambda b: b[3])
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))

def section_node(root, tag):
    return next((n for n in root.iter("node")
                 if n.get("resource-id") == tag and n.get("clickable") == "true"), None)

def section_enabled(root, tag):
    node = section_node(root, tag)
    return node is not None and node.get("enabled") == "true"

def section_status(root):
    node = next((n for n in root.iter("node") if n.get("resource-id") == "section-indicator"), None)
    return node.get("text", "") if node is not None else ""

def turn_section(root, tag):
    node = section_node(root, tag)
    if node is None or node.get("enabled") != "true":
        raise RuntimeError("Section control unavailable: " + tag)
    before = section_status(root)
    tap_bounds([bounds(node)])
    return wait_for(lambda r: section_status(r) != before, "section change after " + tag)

def navigate(label):
    for _ in range(100):
        root = wait_for(lambda r: bool(visible_choices(r, label)) or section_enabled(r, "section-next"), label)
        choices = visible_choices(root, label)
        if choices:
            tap_bounds(choices)
            return
        turn_section(root, "section-next")
    raise RuntimeError("Not reachable by section navigation: " + label)

def reset_section():
    root = hierarchy()
    for _ in range(100):
        if not section_enabled(root, "section-previous"):
            status = section_status(root)
            if status and not status.startswith("1 / "):
                raise RuntimeError("Previous unavailable before first section: " + status)
            return
        root = turn_section(root, "section-previous")
    raise RuntimeError("Could not return to the first section: " + section_status(root))

def verify_fixed_tools():
    reset_section()
    root = wait_for(lambda r: bool(visible_choices(r, "tools-search")), "tools search")
    initial = visible_choices(root, "tools-search")[0]
    before = section_status(root)
    if not section_enabled(root, "section-next"):
        raise RuntimeError("Expected more than one section in the full tool catalog")
    size = re.findall(r"(\d+)x(\d+)", adb("shell", "wm", "size"))
    w, h = map(int, size[-1])
    adb("shell", "input", "swipe", str(w//2), str(h*7//10), str(w//2), str(h*3//10), "400")
    time.sleep(0.5)
    root = hierarchy()
    current = visible_choices(root, "tools-search")
    if not current or current[0] != initial or section_status(root) != before:
        raise RuntimeError("Tools screen moves on vertical swipe")
    root = turn_section(root, "section-next")
    if not section_enabled(root, "section-previous"):
        raise RuntimeError("Next section did not enable previous section")
    reset_section()
    root = wait_for(lambda r: bool(visible_choices(r, "tools-search")), "restored tools search")
    if section_status(root) != before or visible_choices(root, "tools-search")[0] != initial:
        raise RuntimeError("Page controls did not restore Tools")

def start():
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)

def pdf_fixture(title):
    # Minimal valid three-page PDF. No user data or external dependencies.
    objects = [
        b"<< /Type /Catalog /Pages 2 0 R >>",
        b"<< /Type /Pages /Kids [4 0 R 6 0 R 8 0 R] /Count 3 >>",
        b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
    ]
    for index in range(3):
        stream = ("BT /F1 24 Tf 48 760 Td (" + title + ") Tj "
                  "0 -40 Td /F1 12 Tf (pagomi. sample document - page " + str(index + 1) + ") Tj "
                  "0 -30 Td (A sample for scanning and PDF editing.) Tj ET").encode("ascii")
        objects.append(("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                        "/Resources << /Font << /F1 3 0 R >> >> /Contents " + str(5 + 2 * index) + " 0 R >>").encode("ascii"))
        objects.append(b"<< /Length " + str(len(stream)).encode() + b" >>\nstream\n" + stream + b"\nendstream")
    data = bytearray(b"%PDF-1.4\n")
    offsets = [0]
    for index, obj in enumerate(objects, 1):
        offsets.append(len(data))
        data += str(index).encode() + b" 0 obj\n" + obj + b"\nendobj\n"
    startxref = len(data)
    data += ("xref\n0 " + str(len(offsets)) + "\n0000000000 65535 f \n").encode()
    for offset in offsets[1:]:
        data += ("%010d 00000 n \n" % offset).encode()
    data += ("trailer\n<< /Size " + str(len(offsets)) + " /Root 1 0 R >>\nstartxref\n" + str(startxref) + "\n%%EOF\n").encode()
    return bytes(data)

def seed_documents():
    adb("shell", "run-as", APP, "mkdir", "-p", "files/pagomi.")
    for name in ("Invoice_2026", "Meeting_Notes", "Project_Proposal"):
        subprocess.run(["adb", "shell", "-T", "run-as", APP, "tee", "files/pagomi./" + name + ".pdf"],
                       input=pdf_fixture(name.replace("_", " ")), stdout=subprocess.DEVNULL, check=True, timeout=30)

def synthetic_scan():
    # A clean synthetic paper scan with lines, generated exclusively for emulator tests.
    w, h = 420, 594
    rows = []
    for y in range(h):
        row = bytearray()
        for x in range(w):
            header = 45 <= x < 280 and 52 <= y < 68
            line = 45 <= x < (320 if ((y - 115)//26) % 3 else 260) and 115 <= y < 430 and (y - 115) % 26 < 3
            if header:
                row.extend((100, 89, 216))
            elif line:
                row.extend((76, 84, 96))
            else:
                row.extend((255, 255, 255))
        rows.append(b"\x00" + row)
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(b"".join(rows))) + chunk(b"IEND", b"")
    root = adb("shell", "run-as", APP, "pwd").strip()
    rel = "files/scans/gallery-fixture"
    adb("shell", "run-as", APP, "mkdir", "-p", rel)
    def write(name, data):
        subprocess.run(["adb", "shell", "-T", "run-as", APP, "tee", rel + "/" + name],
                       input=data, stdout=subprocess.DEVNULL, check=True, timeout=30)
    write("sample.png", png)
    path = root + "/" + rel + "/sample.png"
    page = {"id": "gallery-page", "kind": "doc", "size": "fit", "shots": [
        {"src": path, "width": w, "height": h, "filter": "original", "rev": 1, "out": path, "outRev": 1, "outW": w, "outH": h}]}
    now = int(time.time() * 1000)
    doc = {"id": "gallery-fixture", "name": "Sample Scan", "created": now, "modified": now,
           "pages": [page], "saved": {"name": "Sample Scan", "pages": [page]}}
    write("doc.json", json.dumps(doc).encode())

def back():
    adb("shell", "input", "keyevent", "4")
    time.sleep(0.7)

def capture_tools():
    for tool_id, (query, title) in TOOL_QUERIES.items():
        reset_section()
        root = wait_for(lambda r: bool(nodes(r, "tools-search")), "tool search")
        if nodes(root, "Clear"):
            navigate("Clear")
        navigate("tools-search")
        adb("shell", "input", "text", query.replace(" ", "%s"))
        # Search uses the Android IME; close it before opening the result.
        adb("shell", "input", "keyevent", "4")
        navigate("tool-" + tool_id)
        capture("tool-" + tool_id, ["tool-screen-" + tool_id])
        back()

def main():
    if adb("shell", "getprop", "ro.kernel.qemu").strip() != "1":
        raise SystemExit("Only a disposable emulator may be reset by this script.")
    OUT.mkdir(parents=True, exist_ok=True)
    apk = Path("app/build/outputs/apk/debug/app-debug.apk")
    adb("install", "-r", str(apk))
    adb("shell", "pm", "clear", APP)
    adb("shell", "cmd", "uimode", "night", "no")
    adb("shell", "wm", "size", "1080x2400")
    adb("shell", "wm", "density", "440")
    adb("shell", "settings", "put", "system", "font_scale", "1.0")
    start()
    capture("onboarding", ["Scan smarter"])
    # Set consent and theme exclusively in the isolated debug fixture, after capturing first launch.
    adb("shell", "am", "force-stop", APP)
    adb("shell", "run-as " + APP + " sh -c 'mkdir -p files; printf \"%s\" \"{\\\"version\\\":2,\\\"at\\\":0}\" > files/consent.json; printf light > files/theme.txt'")
    seed_documents()
    synthetic_scan()
    adb("shell", "pm", "grant", APP, "android.permission.CAMERA")
    start()
    capture("home", ["Scan Document"])
    # Compact dashboards may show fewer recent documents; the library still exposes every file.
    if visible_choices(hierarchy(), "document-Project_Proposal.pdf"):
        navigate("document-Project_Proposal.pdf")
    else:
        navigate("Files")
        navigate("file-Project_Proposal.pdf")
    capture("pdf-editor", ["Edit PDF", "Page 1 of 3"])
    back()
    navigate("Scan")
    capture("scanner", ["Import photos"])
    navigate("Recent scans")
    navigate("Sample Scan")
    capture("review-scan", ["Review Scan"])
    back()
    navigate("Files")
    capture("files", ["My Files"])
    initial = visible_choices(hierarchy(), "files-search")
    if not initial:
        raise RuntimeError("Files search not visible")
    dims = re.findall(r"(\d+)x(\d+)", adb("shell", "wm", "size"))
    w, h = map(int, dims[-1])
    adb("shell", "input", "swipe", str(w//2), str(h*7//10), str(w//2), str(h*3//10), "400")
    if visible_choices(hierarchy(), "files-search") != initial:
        raise RuntimeError("Files header moves on vertical swipe")
    navigate("Tools")
    capture("tools", ["Powerful PDF tools"])
    verify_fixed_tools()
    navigate("Settings")
    capture("settings", ["Manage your preferences, storage and privacy."])
    navigate("Tools")
    capture_tools()
    (OUT / "capture-metadata.json").write_text(json.dumps({
        "commit": os.getenv("GITHUB_SHA", ""),
        "api": adb("shell", "getprop", "ro.build.version.sdk").strip(),
        "display": adb("shell", "wm", "size").strip(),
        "density": adb("shell", "wm", "density").strip(),
        "theme": "light",
        "fixture": "isolated debug install; synthetic three-page PDFs only",
        "screens": sorted(p.stem for p in OUT.glob("*.png")),
        "visual_parity_verified": False
    }, indent=2) + "\n")

if __name__ == "__main__":
    try:
        main()
    except Exception:
        OUT.mkdir(parents=True, exist_ok=True)
        try:
            (OUT / "failure-hierarchy.xml").write_bytes(ET.tostring(hierarchy(), encoding="utf-8"))
            (OUT / "failure-screen.png").write_bytes(adb("exec-out", "screencap", "-p", text=False))
        except Exception as diagnostic_error:
            print("Could not capture failure diagnostics:", diagnostic_error)
        raise
