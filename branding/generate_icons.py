#!/usr/bin/env python3
import base64
import shutil
import struct
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "branding" / "icon.svg"
ANDROID_RES = ROOT / "androidApp" / "src" / "main" / "res"
SHARED_RESOURCE = ROOT / "shared" / "src" / "commonMain" / "composeResources" / "drawable" / "app_icon.png"
DESKTOP_PACKAGING = ROOT / "desktopApp" / "packaging"

ANDROID_CANVAS_DP = 108
ANDROID_CONTENT_DP = 72
ANDROID_DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}

ICO_SIZES = (16, 24, 32, 48, 64, 128, 256)
ICNS_ENTRIES = (
    ("icp4", 16), ("icp5", 32), ("icp6", 64), ("ic07", 128), ("ic08", 256), ("ic09", 512), ("ic10", 1024),
    ("ic11", 32), ("ic12", 64), ("ic13", 256), ("ic14", 512),
)


def render(svg: bytes, size: int) -> bytes:
    return subprocess.run(
        ["rsvg-convert", "--width", str(size), "--height", str(size), "--format", "png"],
        input=svg, capture_output=True, check=True,
    ).stdout


def padded(svg: bytes, canvas: int, content: int) -> bytes:
    offset = (canvas - content) / 2
    data_uri = "data:image/svg+xml;base64," + base64.b64encode(svg).decode()
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" '
        f'width="{canvas}" height="{canvas}" viewBox="0 0 {canvas} {canvas}">'
        f'<image x="{offset}" y="{offset}" width="{content}" height="{content}" xlink:href="{data_uri}"/>'
        f"</svg>"
    ).encode()


def ico(images: dict[int, bytes]) -> bytes:
    header = struct.pack("<HHH", 0, 1, len(images))
    offset = len(header) + 16 * len(images)
    entries, payload = b"", b""
    for size, png in images.items():
        dimension = 0 if size >= 256 else size
        entries += struct.pack("<BBBBHHII", dimension, dimension, 0, 0, 1, 32, len(png), offset + len(payload))
        payload += png
    return header + entries + payload


def icns(images: dict[str, bytes]) -> bytes:
    chunks = b"".join(kind.encode() + struct.pack(">I", len(png) + 8) + png for kind, png in images.items())
    return b"icns" + struct.pack(">I", len(chunks) + 8) + chunks


def write(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    print(path.relative_to(ROOT))


def main() -> None:
    if shutil.which("rsvg-convert") is None:
        sys.exit("rsvg-convert (librsvg) is required")

    svg = SOURCE.read_bytes()

    foreground = padded(svg, ANDROID_CANVAS_DP, ANDROID_CONTENT_DP)
    for density, scale in ANDROID_DENSITIES.items():
        size = round(ANDROID_CANVAS_DP * scale)
        write(ANDROID_RES / f"mipmap-{density}" / "ic_launcher_foreground.png", render(foreground, size))

    large = render(svg, 512)
    write(SHARED_RESOURCE, large)
    write(DESKTOP_PACKAGING / "icon.png", large)
    write(DESKTOP_PACKAGING / "icon.ico", ico({size: render(svg, size) for size in ICO_SIZES}))
    write(DESKTOP_PACKAGING / "icon.icns", icns({kind: render(svg, size) for kind, size in ICNS_ENTRIES}))


if __name__ == "__main__":
    main()
