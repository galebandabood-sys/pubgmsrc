import hashlib
import io
import json
import pathlib
import sys
import tempfile
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from verify_pubg_split import verify, LIBRARY, SPLIT


class BinaryIdentityTests(unittest.TestCase):
    def setUp(self):
        self.workdir = tempfile.TemporaryDirectory()
        self.addCleanup(self.workdir.cleanup)
        self.folder = pathlib.Path(self.workdir.name)
        hdr = bytearray(20)
        hdr[:4] = b"\x7fELF"
        hdr[4:6] = b"\x02\x01"
        hdr[18:20] = (183).to_bytes(2, "little")
        self.binary = bytes(hdr) + b"read only test payload"
        self.record = {
            "package": "com.tencent.ig", "game_version": "4.7.0",
            "version_code": 21740, "abi": "arm64-v8a", "library": LIBRARY,
            "uncompressed_bytes": len(self.binary),
            "sha256": hashlib.sha256(self.binary).hexdigest(),
        }

    def make_split(self, data=None):
        value = io.BytesIO()
        with zipfile.ZipFile(value, "w", compression=zipfile.ZIP_DEFLATED) as z:
            z.writestr(LIBRARY, self.binary if data is None else data)
        return value.getvalue()

    def test_unwrapped_split_matches_binary_only(self):
        path = self.folder / SPLIT
        path.write_bytes(self.make_split())
        result = verify(path, self.record)
        self.assertEqual(result["status"], "MATCHED_BINARY_ONLY")
        self.assertFalse(result["sdk_offsets_verified"])

    def test_outer_apks_bundle_matches(self):
        path = self.folder / "PUBG MOBILE_4.7.0.apks"
        with zipfile.ZipFile(path, "w") as bundle:
            bundle.writestr("split_config.arm64_v8a.apk", self.make_split())
        self.assertEqual(verify(path, self.record)["status"], "MATCHED_BINARY_ONLY")

    def test_wrong_hash_fails(self):
        path = self.folder / SPLIT
        path.write_bytes(self.make_split(self.binary + b"changed"))
        self.assertEqual(verify(path, self.record)["status"], "FAIL")

    def test_wrong_elf_arch_fails(self):
        path = self.folder / "libUE4.so"
        data = bytearray(self.binary)
        data[4] = 1
        path.write_bytes(data)
        self.record["sha256"] = hashlib.sha256(data).hexdigest()
        self.assertEqual(verify(path, self.record)["status"], "FAIL")

    def test_wrong_version_stays_unverified(self):
        path = self.folder / SPLIT
        path.write_bytes(self.make_split())
        self.record["game_version"] = "4.6.0"
        self.assertEqual(verify(path, self.record)["status"], "UNVERIFIED")

    def test_missing_split_stays_unverified(self):
        path = self.folder / "bundle.apks"
        with zipfile.ZipFile(path, "w") as bundle:
            bundle.writestr("base.apk", b"x")
        self.assertEqual(verify(path, self.record)["status"], "UNVERIFIED")


if __name__ == "__main__":
    unittest.main()
