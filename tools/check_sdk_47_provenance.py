#!/usr/bin/env python3
"""Provenance check: the public PUBG 4.5 SDK is NOT a verified PUBG 4.7 SDK.

This read-only report does NOT scan game memory, discover offsets, or patch a
game. It deliberately refuses to mark 4.7 game functionality as supported based
on a successful APK build, version label, or libUE4 checksum alone.
"""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OLD_PUBLIC_SOURCE = "https://github.com/ibrdot/pubgmsrc"
OLD_GIT_BLOBS = {
    "app/src/main/jni/Offset.h": "bf32b4c2a11f06ed37190b06cd739b4506b6fad8",
    "app/src/main/jni/SDK.hpp": "40e1073a44955fde98acc9f8321b1280a5c72c7b",
}
OBSERVED = {
    "package": "com.tencent.ig",
    "version_name": "4.7.0",
    "version_code": 21740,
    "libue4_sha256": "9e5b96f709d38a2eaf7f186487fb8e2430e45593b7e33d7d5fd51eca83d78e3a",
}


def git_blob_sha(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\x00" + data).hexdigest()


def audit(root=ROOT):
    files = {}
    for rel, expected in OLD_GIT_BLOBS.items():
        path = root / rel
        if not path.is_file():
            files[rel] = {"status": "MISSING"}
        else:
            actual = git_blob_sha(path.read_bytes())
            files[rel] = {
                "status": "EXACT_PUBLIC_4_5_BASELINE" if actual == expected else "CHANGED_NOT_VERIFIED",
                "git_blob": actual,
                "public_4_5_git_blob": expected,
            }

    # Source provenance does not establish the ABI/layout/function offsets for 4.7.
    # Never label functionality VERIFIED solely because old source builds successfully.
    return {
        "target": OBSERVED,
        "upstream": OLD_PUBLIC_SOURCE,
        "source_files": files,
        "sdk_4_7_compatible": False,
        "esp_4_7_verified": False,
        "aimbot_4_7_verified": False,
        "weapon_features_4_7_verified": False,
        "reason": "No independently tested 4.7 SDK layout/function profile for the exact game binary is present.",
        "necessary_next_evidence": [
            "A 4.7 SDK profile tied to the exact libUE4 build",
            "Independent validation of relevant object layouts and function addresses",
            "Separate on-device tests before any capability is called working",
        ],
        "note": "False means NOT VERIFIED, not proof that no implementation could ever work.",
    }


if __name__ == "__main__":
    print(json.dumps(audit(), indent=2, ensure_ascii=False))
