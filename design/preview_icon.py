#!/usr/bin/env python3
"""Render a preview sheet of the launcher icon (square, circle, squircle).

Developer helper only: the app itself ships the adaptive icon as Android vector
drawables (app/src/main/res/drawable/ic_launcher_{foreground,background}.xml).

This script draws the *same geometry and colours* as design/icon.svg with plain
Pillow, so it needs no Cairo/Chromium and works on a machine with no native
rendering libraries:

    pip install pillow
    python3 design/preview_icon.py

If you change design/icon.svg, keep the coordinates below in sync.
"""
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "design", "icon-preview.png")

SIZE = 512          # final panel size in pixels
SS = 4              # supersampling factor
N = SIZE * SS       # canvas size while drawing
VIEW = 108.0        # SVG viewBox width

# The icon body is drawn inside <g transform="translate(9.72,9.72) scale(0.82)">
TX, TY, TS = 9.72, 9.72, 0.82


def s(value):
    """SVG user unit -> device pixels, including the group transform."""
    return (TX + TS * value) * N / VIEW


def hex_rgba(value, alpha=1.0):
    value = value.lstrip("#")
    r, g, b = (int(value[i:i + 2], 16) for i in (0, 2, 4))
    return (r, g, b, int(round(255 * alpha)))


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(len(a)))


def cubic(p0, p1, p2, p3, steps=64):
    pts = []
    for i in range(steps + 1):
        t = i / steps
        u = 1 - t
        x = u ** 3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t ** 3 * p3[0]
        y = u ** 3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t ** 3 * p3[1]
        pts.append((s(x), s(y)))
    return pts


def quad(p0, p1, p2, steps=48):
    pts = []
    for i in range(steps + 1):
        t = i / steps
        u = 1 - t
        x = u * u * p0[0] + 2 * u * t * p1[0] + t * t * p2[0]
        y = u * u * p0[1] + 2 * u * t * p1[1] + t * t * p2[1]
        pts.append((s(x), s(y)))
    return pts


def arc(cx, cy, r, a0, a1, steps=64):
    return [
        (s(cx + r * math.cos(math.radians(a0 + (a1 - a0) * i / steps))),
         s(cy + r * math.sin(math.radians(a0 + (a1 - a0) * i / steps))))
        for i in range(steps + 1)
    ]


def stroke(draw, points, width, color):
    w = max(1, int(round(width * N / VIEW)))
    draw.line(points, fill=color, width=w, joint="curve")
    cap = w / 2.0
    for x, y in (points[0], points[-1]):
        draw.ellipse((x - cap, y - cap, x + cap, y + cap), fill=color)


def circle(draw, cx, cy, r, color):
    draw.ellipse((s(cx - r), s(cy - r), s(cx + r), s(cy + r)), fill=color)


def background():
    """<rect> with the #EAF9FF -> #9CD9F0 -> #55B2DA linear gradient."""
    img = Image.new("RGB", (N, N))
    stops = [(0.0, hex_rgba("#EAF9FF")[:3]),
             (0.55, hex_rgba("#9CD9F0")[:3]),
             (1.0, hex_rgba("#55B2DA")[:3])]
    px = img.load()
    for y in range(N):
        for_ = y / max(1, N - 1)
        for x in range(0, N, 1):
            t = min(1.0, max(0.0, 0.55 * (x / (N - 1)) + 0.85 * for_))
            for i in range(len(stops) - 1):
                a, ca = stops[i]
                b, cb = stops[i + 1]
                if a <= t <= b:
                    px[x, y] = lerp(ca, cb, (t - a) / (b - a) if b > a else 0)
                    break
            else:
                px[x, y] = stops[-1][1]
    return img.convert("RGBA")


def droplet_layer():
    """The droplet body with its soft radial highlight."""
    mask = Image.new("L", (N, N), 0)
    md = ImageDraw.Draw(mask)
    body = cubic((54, 30.5), (45, 43), (34, 55), (34, 64.5))
    body += arc(54, 64.5, 20, 180, 0)[1:]
    body += cubic((74, 64.5), (74, 55), (63, 43), (54, 30.5))
    md.polygon(body, fill=255)

    grad = Image.new("RGBA", (N, N))
    gd = ImageDraw.Draw(grad)
    cx, cy, r = s(46), s(52), s(44)
    for i in range(96, 0, -1):
        t = i / 96.0
        if t <= 0.6:
            color = lerp(hex_rgba("#FFFFFF"), hex_rgba("#EFF9FD"), t / 0.6)
        else:
            color = lerp(hex_rgba("#EFF9FD"), hex_rgba("#B9DFF2"), (t - 0.6) / 0.4)
        rad = r * t
        gd.ellipse((cx - rad, cy - rad, cx + rad, cy + rad), fill=color)
    grad.putalpha(mask)
    return grad


def draw_icon():
    img = background()
    draw = ImageDraw.Draw(img, "RGBA")

    # steam wisps
    white = (255, 255, 255, 242)
    stroke(draw, cubic((48, 28), (44, 23.5), (44.5, 17.5), (49, 14), 40), 3.2, white)
    stroke(draw, cubic((60, 28), (64, 23.5), (63.5, 17.5), (59, 14), 40), 3.2, white)

    # droplet
    img.alpha_composite(droplet_layer())
    draw = ImageDraw.Draw(img, "RGBA")

    # gloss on the left shoulder
    stroke(draw, cubic((45.5, 48), (43.2, 52.5), (42, 57), (42, 61), 40), 3.0,
           hex_rgba("#FFFFFF", 0.9))

    # cheeks
    circle(draw, 41, 69.5, 3.8, hex_rgba("#FF8FA8", 0.45))
    circle(draw, 67, 69.5, 3.8, hex_rgba("#FF8FA8", 0.45))

    # eyes + highlights
    ink = hex_rgba("#16404F")
    circle(draw, 47, 63.5, 3.4, ink)
    circle(draw, 61, 63.5, 3.4, ink)
    circle(draw, 45.9, 62.2, 1.35, (255, 255, 255, 255))
    circle(draw, 59.9, 62.2, 1.35, (255, 255, 255, 255))

    # smile
    stroke(draw, quad((47.6, 71.6), (54, 77.6), (60.4, 71.6), 40), 2.8, ink)

    # sparkle
    star = [(73.5, 35.5), (74.8, 38.6), (77.9, 39.9), (74.8, 41.2),
            (73.5, 44.3), (72.2, 41.2), (69.1, 39.9), (72.2, 38.6)]
    draw.polygon([(s(x), s(y)) for x, y in star], fill=(255, 255, 255, 217))
    return img


def masked(raw, shape):
    mask = Image.new("L", (SIZE, SIZE), 0)
    draw = ImageDraw.Draw(mask)
    if shape == "circle":
        draw.ellipse((0, 0, SIZE, SIZE), fill=255)
    elif shape == "squircle":
        draw.rounded_rectangle((0, 0, SIZE, SIZE), radius=int(SIZE * 0.3), fill=255)
    else:
        draw.rectangle((0, 0, SIZE, SIZE), fill=255)
    tile = Image.new("RGBA", (SIZE, SIZE), (246, 248, 250, 255))
    tile.paste(raw, (0, 0), mask)
    return tile


def main():
    raw = draw_icon().resize((SIZE, SIZE), Image.LANCZOS)
    gap = 24
    panels = [masked(raw, shape) for shape in ("square", "circle", "squircle")]
    canvas = Image.new("RGB", (SIZE * 3 + gap * 4, SIZE + gap * 2), (246, 248, 250))
    for i, panel in enumerate(panels):
        canvas.paste(panel, (gap + i * (SIZE + gap), gap))
    canvas.save(OUT)
    print("wrote", OUT, canvas.size)


if __name__ == "__main__":
    main()
