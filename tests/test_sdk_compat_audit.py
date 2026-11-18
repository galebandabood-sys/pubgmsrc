import hashlib
import json
import pathlib
import sys
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
from sdk_compat_audit import REQUIRED, check, parse_offsets

class CompatibilityTests(unittest.TestCase):
    def setUp(self):
        self.t = tempfile.TemporaryDirectory()
        self.addCleanup(self.t.cleanup)
        self.root = pathlib.Path(self.t.name)
        self.offsets = self.root / 'Offset.h'
        self.offsets.write_text(''.join(f'#define {k} 0x{i + 50:X}\n' for i, k in enumerate(REQUIRED)))
        self.profile = self.root / 'profile.json'
        self.elf = self.root / 'libUE4.so'
        header = bytearray(20)
        header[:4] = b'\x7fELF'
        header[4:6] = b'\x02\x01'
        header[18:20] = (183).to_bytes(2, 'little')
        self.elf.write_bytes(bytes(header) + b'valid fixture')
        self.data = {
            'schema': 1, 'package': 'com.tencent.ig', 'game_version': '4.7.0',
            'abi': 'arm64-v8a', 'source_url': 'fixture://test',
            'libue4_sha256': hashlib.sha256(self.elf.read_bytes()).hexdigest(),
            'offsets': {key: hex(i + 50) for i, key in enumerate(REQUIRED)}
        }
        self.save()

    def save(self):
        self.profile.write_text(json.dumps(self.data))

    def test_no_profile_is_unverified(self):
        self.assertEqual(check(None, self.offsets, None, 'com.tencent.ig', '4.7.0')['status'], 'UNVERIFIED')

    def test_no_binary_is_unverified(self):
        self.assertEqual(check(self.profile, self.offsets, None, 'com.tencent.ig', '4.7.0')['status'], 'UNVERIFIED')

    def test_exact_metadata_match_only(self):
        self.assertEqual(check(self.profile, self.offsets, self.elf, 'com.tencent.ig', '4.7.0')['status'], 'MATCHED_METADATA_ONLY')

    def test_other_edition_fails(self):
        self.assertEqual(check(self.profile, self.offsets, self.elf, 'com.pubg.krmobile', '4.7.0')['status'], 'FAIL')

    def test_other_version_fails(self):
        self.assertEqual(check(self.profile, self.offsets, self.elf, 'com.tencent.ig', '4.6.0')['status'], 'FAIL')

    def test_offset_mismatch_fails(self):
        self.data['offsets']['GNames_Offset'] = '0x987654'
        self.save()
        result = check(self.profile, self.offsets, self.elf, 'com.tencent.ig', '4.7.0')
        self.assertEqual(result['status'], 'FAIL')
        self.assertTrue(any('Offset mismatch' in i for i in result['issues']))

    def test_wrong_binary_hash_fails(self):
        self.elf.write_bytes(self.elf.read_bytes() + b'altered')
        self.assertEqual(check(self.profile, self.offsets, self.elf, 'com.tencent.ig', '4.7.0')['status'], 'FAIL')

    def test_wrong_arch_fails(self):
        value = bytearray(self.elf.read_bytes())
        value[4] = 1
        self.elf.write_bytes(value)
        self.data['libue4_sha256'] = hashlib.sha256(value).hexdigest()
        self.save()
        self.assertEqual(check(self.profile, self.offsets, self.elf, 'com.tencent.ig', '4.7.0')['status'], 'FAIL')

    def test_parse_legacy_constants(self):
        self.assertEqual(parse_offsets(self.offsets)['GNames_Offset'], 50)

if __name__ == '__main__':
    unittest.main()
