"""Gera data/arcane/structures/empty.nbt: estrutura vazia 7x5x7 usada pelos GameTests.

Uso:  python tools/gen_test_structure.py
O GameTest coloca a estrutura e monta o cenario em codigo (ver ArcaneGameTests.java).
"""
import gzip
import struct
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / "src/main/resources/data/arcane/structures/empty.nbt"
DATA_VERSION = 3465  # Minecraft 1.20.1


def name(s: str) -> bytes:
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def tag_int(key, v):
    return b"\x03" + name(key) + struct.pack(">i", v)


def tag_string(key, v):
    return b"\x08" + name(key) + name(v)


def tag_list(key, elem_type, payloads):
    return b"\x09" + name(key) + bytes([elem_type]) + struct.pack(">i", len(payloads)) + b"".join(payloads)


def main():
    size = [struct.pack(">i", v) for v in (7, 5, 7)]
    palette = [tag_string("Name", "minecraft:air") + b"\x00"]
    root = (tag_int("DataVersion", DATA_VERSION)
            + tag_list("size", 3, size)
            + tag_list("palette", 10, palette)
            + tag_list("blocks", 10, [])
            + tag_list("entities", 10, []))
    data = b"\x0a" + name("") + root + b"\x00"
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(gzip.compress(data))
    print("  ", OUT)


if __name__ == "__main__":
    main()
