#!/usr/bin/env python3
"""Read-only verification of legacy PUBG 4.5 offsets in a local 64-bit ARM ELF."""
import argparse
import json
import pathlib
import struct

LEGACY = {
    "GNames_Offset": 0x8939470,
    "GUObject_Offset": 0xef3b3f0,
    "ShootBulletInner_Offset": 0x6dfe100,
    "GetMuzzleTransform_Offset": 0x6e17390,
    "ProcessEvent_Offset": 0x8bac0a8,
    "GetActorArray": 0xa6ca440,
    "GNativeAndroidApp_Offset": 0xec732a8,
}

def inspect(path):
    with path.open("rb") as f:
        header = f.read(64)
        if len(header) < 64 or header[:6] != b"\x7fELF\x02\x01":
            raise ValueError("Expected little-endian ELF64")
        elf = struct.unpack("<16sHHIQQQIHHHHHH", header)
        if elf[2] != 183:
            raise ValueError("Expected AArch64 e_machine=183")
        phoff, entsize, num = elf[5], elf[9], elf[10]
        loads = []
        for index in range(num):
            f.seek(phoff + index * entsize)
            t, flags, offset, va, _, size, mem_size, _ = struct.unpack(
                "<IIQQQQQQ", f.read(56)
            )
            if t == 1:
                loads.append((va, mem_size, size, offset, flags))
        findings = []
        for name, addr in LEGACY.items():
            seg = next((s for s in loads if s[0] <= addr < s[0] + s[1]), None)
            if seg is None:
                findings.append({"name": name, "offset": hex(addr), "segment": "NOT_MAPPED"})
                continue
            va, mem_size, file_size, offset, flags = seg
            access = "".join(ch if flags & bit else "-" for ch, bit in (("R",4),("W",2),("X",1)))
            if addr < va + file_size:
                f.seek(offset + addr - va)
                head16 = f.read(16).hex()
            else:
                head16 = None
            findings.append({"name": name, "offset": hex(addr), "segment": access, "head16": head16})
        return {
            "target": "PUBG Global 4.7.0 build 21740",
            "game_feature_support": "NOT_VERIFIED",
            "offsets": findings,
            "warning": "Segment membership is NOT function identity. Do not enable legacy hooks based on this result.",
        }

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--binary", required=True, type=pathlib.Path)
    args = parser.parse_args()
    print(json.dumps(inspect(args.binary), ensure_ascii=False, indent=2))

if __name__ == "__main__":
    main()
