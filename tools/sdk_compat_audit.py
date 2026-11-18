#!/usr/bin/env python3
"""Read-only evidence check for matching a generated UE4 SDK profile to an Android build.

This does not load a process, dump memory, inject code, or generate offsets.
A metadata match is NOT evidence that any game feature actually works.
"""
import argparse
import hashlib
import json
import pathlib
import re
import sys

EXPECTED_ABI = 'arm64-v8a'
REQUIRED = (
    'GNames_Offset', 'GUObject_Offset', 'ShootBulletInner_Offset',
    'GetMuzzleTransform_Offset', 'ProcessEvent_Offset', 'GetActorArray',
    'GNativeAndroidApp_Offset', 'Actors_Offset'
)
PACKAGE_IDS = {
    'com.tencent.ig', 'com.pubg.krmobile', 'com.vng.pubgmobile',
    'com.pubg.imobile', 'com.rekoo.pubgm'
}

def parse_offsets(path):
    text = path.read_text(encoding='utf-8')
    result = {}
    for name, value in re.findall(r'^\s*#\s*define\s+([A-Za-z_]\w*)\s+(0[xX][0-9a-fA-F]+|\d+)\s*(?:$|//)', text, re.M):
        result[name] = int(value, 0)
    return result

def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for block in iter(lambda: f.read(1 << 20), b''):
            h.update(block)
    return h.hexdigest()

def is_64bit_arm_elf(path):
    with path.open('rb') as f:
        header = f.read(20)
    return (len(header) == 20 and header[:4] == b'\x7fELF' and
            header[4] == 2 and header[5] == 1 and
            int.from_bytes(header[18:20], 'little') == 183)

def check(profile_path, offsets_path, library_path, package, version):
    issues = []
    details = {}
    if not profile_path or not profile_path.is_file():
        return {'status': 'UNVERIFIED', 'reason': 'No build-specific SDK profile supplied',
                'issues': ['Missing profile JSON for this exact game package and version'],
                'details': {'target_package': package, 'target_version': version}}
    try:
        profile = json.loads(profile_path.read_text(encoding='utf-8'))
    except (OSError, ValueError) as error:
        return {'status': 'FAIL', 'reason': 'Invalid SDK profile',
                'issues': [str(error)], 'details': {}}
    if not isinstance(profile, dict):
        return {'status': 'FAIL', 'reason': 'Invalid SDK profile structure',
                'issues': ['Expected JSON object'], 'details': {}}
    details['package'] = profile.get('package')
    details['version'] = profile.get('game_version')
    details['abi'] = profile.get('abi')
    details['source'] = profile.get('source_url')
    if package not in PACKAGE_IDS:
        issues.append('Unsupported game package: ' + package)
    if profile.get('schema') != 1:
        issues.append('Unsupported profile schema (expected 1)')
    if profile.get('package') != package:
        issues.append('Game package does not match profile')
    if profile.get('game_version') != version:
        issues.append('Game version does not match profile')
    if profile.get('abi') != EXPECTED_ABI:
        issues.append('ABI is not arm64-v8a')
    values = profile.get('offsets')
    if not isinstance(values, dict):
        issues.append('Profile offsets must be an object')
        values = {}
    try:
        compiled = parse_offsets(offsets_path)
    except OSError as error:
        issues.append('Cannot read native Offset.h: ' + str(error))
        compiled = {}
    for name in REQUIRED:
        original = compiled.get(name)
        candidate = values.get(name)
        if original is None:
            issues.append('Missing C++ offset constant: ' + name)
            continue
        if not isinstance(candidate, str) or not re.fullmatch(r'0[xX][0-9a-fA-F]+', candidate):
            issues.append('Missing/invalid profile offset: ' + name)
            continue
        if int(candidate, 16) != original:
            issues.append('Offset mismatch ' + name + ': source=' + hex(original) +
                          ' profile=' + candidate)
    expected_hash = profile.get('libue4_sha256', '')
    if (not re.fullmatch(r'[0-9a-fA-F]{64}', str(expected_hash)) or
            str(expected_hash).lower() == '0' * 64):
        issues.append('Missing/invalid SHA-256 for exact libUE4.so build')
    if not library_path or not library_path.is_file():
        return {'status': 'UNVERIFIED', 'reason': 'Cannot verify engine binary identity',
                'issues': issues + ['Exact libUE4.so binary not supplied for fingerprint match'],
                'details': details}
    if not is_64bit_arm_elf(library_path):
        issues.append('Library is not an ARM64 little-endian ELF shared object')
    actual_hash = digest(library_path)
    details['libue4_sha256'] = actual_hash
    if actual_hash.lower() != str(expected_hash).lower():
        issues.append('libUE4.so SHA-256 does not match profile')
    if issues:
        return {'status': 'FAIL', 'reason': 'Build and SDK data disagree',
                'issues': issues, 'details': details}
    return {'status': 'MATCHED_METADATA_ONLY',
            'reason': 'SDK constants and ELF fingerprint match; game behavior NOT verified',
            'issues': [], 'details': details}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--offsets', type=pathlib.Path, required=True)
    parser.add_argument('--profile', type=pathlib.Path)
    parser.add_argument('--libue4', type=pathlib.Path)
    parser.add_argument('--package', required=True)
    parser.add_argument('--version', required=True)
    parser.add_argument('--output', type=pathlib.Path)
    args = parser.parse_args()
    result = check(args.profile, args.offsets, args.libue4, args.package, args.version)
    raw = json.dumps(result, ensure_ascii=False, indent=2) + '\n'
    if args.output:
        args.output.write_text(raw, encoding='utf-8')
    sys.stdout.write(raw)
    return 0 if result['status'] == 'MATCHED_METADATA_ONLY' else 2

if __name__ == '__main__':
    sys.exit(main())
