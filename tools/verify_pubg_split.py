#!/usr/bin/env python3
"""Read-only check that an installed PUBG split APK contains a known libUE4.so.

Accepts an extracted .apks (outer ZIP containing split APKs), an individual
split_config.arm64_v8a.apk, or an already extracted libUE4.so. No game memory
access, root, binary patching or kernel module loading. Matching this binary
does NOT establish SDK offsets or ESP/aimbot compatibility.
"""
import argparse
import hashlib
import json
import pathlib
import re
import shutil
import sys
import tempfile
import zipfile


LIBRARY = "lib/arm64-v8a/libUE4.so"
SPLIT = "split_config.arm64_v8a.apk"
ELF_ARM64_MAGIC = b"\x7fELF"


def verify_stream(stream, expected_sha, expected_size):
    digest = hashlib.sha256()
    header = b""
    size = 0
    while True:
        chunk = stream.read(1024 * 1024)
        if not chunk:
            break
        if not header:
            header = chunk[:20]
        digest.update(chunk)
        size += len(chunk)
    actual_hash = digest.hexdigest()
    arm64 = (len(header) >= 20 and header[:4] == ELF_ARM64_MAGIC
             and header[4] == 2 and header[5] == 1
             and int.from_bytes(header[18:20], "little") == 183)
    issues = []
    if actual_hash != expected_sha.lower():
        issues.append("SHA-256 differs from recorded build")
    if size != expected_size:
        issues.append("Uncompressed library size differs from recorded build")
    if not arm64:
        issues.append("Library header is not ARM64 little-endian ELF")
    return {
        "status": "MATCHED_BINARY_ONLY" if not issues else "FAIL",
        "binary_sha256": actual_hash,
        "uncompressed_bytes": size,
        "arm64_elf": arm64,
        "issues": issues,
        "sdk_offsets_verified": False,
        "game_features_verified": False,
    }


def verify(input_path, reference):
    required = {
        "package": "com.tencent.ig",
        "game_version": "4.7.0",
        "version_code": 21740,
        "abi": "arm64-v8a",
        "library": LIBRARY,
    }
    for key, value in required.items():
        if reference.get(key) != value:
            return {"status": "UNVERIFIED", "reason": "Reference metadata mismatch: " + key}
    sha = reference.get("sha256", "")
    size = reference.get("uncompressed_bytes")
    if not isinstance(sha, str) or not re.fullmatch(r"[a-fA-F0-9]{64}", sha) or type(size) is not int or size <= 0:
        return {"status": "UNVERIFIED", "reason": "Invalid reference SHA-256 or uncompressed size"}
    if not input_path.is_file():
        return {"status": "UNVERIFIED", "reason": "Input file not found"}
    try:
        if input_path.suffix.lower() == ".so":
            with input_path.open("rb") as stream:
                return verify_stream(stream, sha, size)
        with zipfile.ZipFile(input_path) as archive:
            if input_path.suffix.lower() == ".apks":
                candidates = [name for name in archive.namelist()
                              if name.rsplit("/", 1)[-1] == SPLIT]
                if len(candidates) != 1:
                    return {"status": "UNVERIFIED", "reason": "Expected exactly one ARM64 split APK in bundle"}
                # Stream split data to temporary disk; never load ~120 MB APK into RAM.
                with tempfile.TemporaryFile() as temp:
                    with archive.open(candidates[0]) as nested:
                        shutil.copyfileobj(nested, temp, 1024 * 1024)
                    temp.seek(0)
                    with zipfile.ZipFile(temp) as split:
                        with split.open(LIBRARY) as lib:
                            return verify_stream(lib, sha, size)
            else:
                with archive.open(LIBRARY) as lib:
                    return verify_stream(lib, sha, size)
    except (OSError, zipfile.BadZipFile, KeyError, RuntimeError, ValueError) as exc:
        return {"status": "UNVERIFIED", "reason": "Cannot read archive/library: " + str(exc)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=pathlib.Path, required=True,
                        help="Path to .apks bundle, arm64 split .apk or libUE4.so")
    parser.add_argument("--reference", type=pathlib.Path, required=True,
                        help="Evidence JSON file with exact version and SHA-256")
    args = parser.parse_args()
    try:
        reference = json.loads(args.reference.read_text(encoding="utf-8"))
        if not isinstance(reference, dict):
            raise ValueError("Reference must be a JSON object")
        result = verify(args.input, reference)
    except (OSError, ValueError) as exc:
        result = {"status": "UNVERIFIED", "reason": "Cannot read reference: " + str(exc)}
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result["status"] == "MATCHED_BINARY_ONLY" else 2


if __name__ == "__main__":
    sys.exit(main())
