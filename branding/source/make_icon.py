"""Draws the BarelySorts icon as 32x32 pixel art and writes it at several sizes.

Run from the repository root: python branding/source/make_icon.py
Writes branding/icon/icon_<size>.png, branding/icon/icon.svg and barelysorts_logo.png (for the jar).
"""
import math
from pathlib import Path

from PIL import Image

N = 32
OUT = Path(__file__).resolve().parent.parent / "icon"

# Palette (shared with the other ItMeCasey Studios mods)
BG = (24, 22, 38)
BG_EDGE = (46, 42, 70)
BG_TOP = (58, 54, 88)
GOLD = (242, 179, 61)
GOLD_HI = (255, 226, 140)
GOLD_LO = (178, 116, 32)
PANEL = (34, 31, 54)
BUBBLE = (250, 250, 250)
BUBBLE_LO = (196, 200, 214)
INK = (24, 22, 38)

px = [[None] * N for _ in range(N)]


def put(x, y, c):
    if 0 <= x < N and 0 <= y < N:
        px[y][x] = c


# Background tile with stepped rounded corners.
R = 4
for y in range(N):
    for x in range(N):
        cx = min(max(x, R - 0.5), N - R - 0.5)
        cy = min(max(y, R - 0.5), N - R - 0.5)
        if math.hypot(x - cx, y - cy) <= R:
            px[y][x] = BG
for y in range(N):
    for x in range(N):
        if px[y][x] is None:
            continue
        edge = any(
            not (0 <= x + dx < N and 0 <= y + dy < N) or px[y + dy][x + dx] is None
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
        )
        if edge:
            px[y][x] = BG_TOP if y < N // 2 else BG_EDGE

# Sort glyph, as drawn on the in-game button: bars of falling length, in a bevelled gold.
WIDTHS = [22, 18, 14, 10, 6]
BAR_H, GAP = 3, 2
total = len(WIDTHS) * BAR_H + (len(WIDTHS) - 1) * GAP
top = (N - total) // 2
left = 5
for i, w in enumerate(WIDTHS):
    y0 = top + i * (BAR_H + GAP)
    for dy in range(BAR_H):
        for dx in range(w):
            c = GOLD
            if dy == 0:
                c = GOLD_HI
            elif dy == BAR_H - 1:
                c = GOLD_LO
            if dx == w - 1 and dy > 0:
                c = GOLD_LO
            put(left + dx, y0 + dy, c)

# Write the base image.
img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
for y in range(N):
    for x in range(N):
        if px[y][x] is not None:
            img.putpixel((x, y), px[y][x] + (255,))

OUT.mkdir(parents=True, exist_ok=True)
for size in (32, 64, 128, 256, 512, 1024):
    img.resize((size, size), Image.NEAREST).save(OUT / f"icon_{size}.png", optimize=True)
img.resize((512, 512), Image.NEAREST).save(OUT / "barelysorts_logo.png", optimize=True)

# SVG version: one rect per pixel, crisp at any size.
rects = []
for y in range(N):
    for x in range(N):
        c = px[y][x]
        if c is not None:
            rects.append(f'<rect x="{x}" y="{y}" width="1.02" height="1.02" fill="#{c[0]:02x}{c[1]:02x}{c[2]:02x}"/>')
(OUT / "icon.svg").write_text(
    f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {N} {N}" shape-rendering="crispEdges">'
    + "".join(rects) + "</svg>\n",
    encoding="utf-8",
)
print("wrote", OUT)
