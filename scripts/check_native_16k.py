#!/usr/bin/env python3
"""Check 64-bit native ELF LOAD segment alignment for Google Play 16 KB devices.

Usage: python3 scripts/check_native_16k.py path/to/app.apk
Uses Python's standard library only; no native host dependencies.
"""
from __future__ import annotations

import struct
import sys
import zipfile
from pathlib import Path

REQUIRED = 0x4000

def inspect_so(data: bytes, name: str) -> list[str]:
    if data[:4] != b"\x7fELF":
        return [f"{name}: missing ELF magic"]
    ei_class, ei_data = data[4], data[5]
    if ei_class != 2:
        return []  # 16 KB Google Play requirement applies to 64-bit native libraries
    endian = "<" if ei_data == 1 else ">"
    if len(data) < 64:
        return [f"{name}: truncated ELF header"]
    phoff = struct.unpack_from(endian + "Q", data, 32)[0]
    entsize, count = struct.unpack_from(endian + "HH", data, 54)
    if entsize < 56:
        return [f"{name}: invalid ELF program header size"]
    errors = []
    seen_load = False
    for i in range(count):
        offset = phoff + i * entsize
        if offset + 56 > len(data):
            return [f"{name}: truncated ELF program headers"]
        p_type = struct.unpack_from(endian + "I", data, offset)[0]
        if p_type != 1:  # PT_LOAD
            continue
        seen_load = True
        alignment = struct.unpack_from(endian + "Q", data, offset + 48)[0]
        if alignment < REQUIRED:
            errors.append(f"{name}: PT_LOAD #{i} alignment {alignment:#x}, expected >= {REQUIRED:#x}")
    return errors if seen_load else [f"{name}: no PT_LOAD segments"]

def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: check_native_16k.py path/to/app.apk", file=sys.stderr)
        return 2
    file = Path(sys.argv[1])
    errors = []
    checked = 0
    with zipfile.ZipFile(file) as apk:
        for name in sorted(apk.namelist()):
            if not (name.startswith("lib/arm64-v8a/") or name.startswith("lib/x86_64/")):
                continue
            if not name.endswith(".so"):
                continue
            checked += 1
            errors += inspect_so(apk.read(name), name)
    print(f"Checked {checked} 64-bit native libraries in {file.name}.")
    if errors:
        for error in errors:
            print("ERROR:", error, file=sys.stderr)
        return 1
    print("ELF program-header alignment check passed; verify 16 KB page size on a real device too.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
