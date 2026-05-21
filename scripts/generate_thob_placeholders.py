# /// script
# requires-python = ">=3.10"
# dependencies = [
#   "Pillow>=10.0",
#   "arabic-reshaper>=3.0",
#   "python-bidi>=0.4.2",
# ]
# ///
"""Generate placeholder images for the Sudanese toub category.

Each placeholder is a 1080x1440 portrait JPEG with:
  - A vertical gradient backdrop using the toub's accent color
  - A stylised silhouette of a woman wearing a toub (head + trapezoid)
  - An Arabic title naming the sub-type
  - A small footer reminding the user this is a placeholder

The trapezoid drawn on the silhouette is exactly the region whose
normalised corners (top-left, top-right, bottom-right, bottom-left)
are emitted to templates.json — so the runtime template compositor
will warp user artwork onto the same area we have visually marked.
"""
from __future__ import annotations

import json
import sys
from dataclasses import dataclass
from pathlib import Path

# Force UTF-8 so the Arabic strings + status glyphs print on Windows cp1252 consoles.
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
sys.stderr.reconfigure(encoding="utf-8", errors="replace")

import arabic_reshaper
from bidi.algorithm import get_display
from PIL import Image, ImageDraw, ImageFont

# ─────────────────────────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────────────────────────
REPO = Path(__file__).resolve().parents[1]
OUT_DIR = REPO / "app" / "src" / "main" / "assets" / "templates" / "thob_sudani"
FONT_DIR = REPO / "app" / "src" / "main" / "res" / "font"

W, H = 1080, 1440  # portrait, matches card_morning.jpg aspect

# Trapezoid for the toub body (normalised 0..1, in image coordinates).
# Order: top-left, top-right, bottom-right, bottom-left.
QUAD = ((0.30, 0.32), (0.70, 0.32), (0.80, 0.86), (0.20, 0.86))


@dataclass(frozen=True)
class ToubStyle:
    file_id: str
    title_ar: str        # the display name
    accent_hex: str      # primary fabric colour
    accent2_hex: str     # secondary / pattern colour
    blend: str           # blend mode for templates.json
    alpha: float         # blend alpha for templates.json


STYLES: tuple[ToubStyle, ...] = (
    # توب الرقمة — embroidered, deep red on cream
    ToubStyle("toub_raqma_1",   "توب الرقمة",   "#C9A24B", "#8B2F0F", "FABRIC_REALISTIC", 0.82),
    # توب الفتلة — gold thread on champagne
    ToubStyle("toub_fatla_1",   "توب الفتلة",   "#D4AF37", "#FFF0F5", "FABRIC_REALISTIC", 0.85),
    # توب الفردة — ceremonial, pure ivory with rose accent
    ToubStyle("toub_farda_1",   "توب الفردة",   "#F5E6C8", "#E8A7B5", "OVERLAY",          0.78),
    # توب الزراف — silk, deep rose with lavender highlight
    ToubStyle("toub_zaraf_1",   "توب الزراف",   "#E0294A", "#9B59B6", "FABRIC_REALISTIC", 0.80),
    # توب العرس — wedding special (the gift moment 💍)
    ToubStyle("toub_wedding_1", "توب العرس",    "#FFF0F5", "#D4AF37", "OVERLAY",          0.82),
)

FOOTER_AR = "صورة مؤقتة — استبدلها بصورة حقيقية"
BRAND_AR = "مَواعي"


# ─────────────────────────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────────────────────────
def hex_to_rgb(hx: str) -> tuple[int, int, int]:
    hx = hx.lstrip("#")
    return tuple(int(hx[i:i + 2], 16) for i in (0, 2, 4))


def mix(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def reshape(text: str) -> str:
    """Apply Arabic reshaping + bidi so PIL renders RTL glyphs correctly."""
    return get_display(arabic_reshaper.reshape(text))


def load_font(name: str, size: int) -> ImageFont.FreeTypeFont:
    path = FONT_DIR / name
    if not path.exists():
        raise FileNotFoundError(f"Missing font: {path}")
    return ImageFont.truetype(str(path), size)


def draw_gradient(img: Image.Image, top: tuple[int, int, int], bottom: tuple[int, int, int]) -> None:
    """Vertical gradient — top → bottom across the whole canvas."""
    px = img.load()
    for y in range(H):
        t = y / (H - 1)
        c = mix(top, bottom, t)
        for x in range(W):
            px[x, y] = c


def draw_silhouette(
    draw: ImageDraw.ImageDraw,
    accent: tuple[int, int, int],
    accent2: tuple[int, int, int],
) -> tuple[tuple[float, float], ...]:
    """Draw head + toub trapezoid. Return the trapezoid's pixel corners."""
    # Head: circle just above the trapezoid top edge.
    head_r = int(W * 0.085)
    head_cx, head_cy = W // 2, int(H * 0.25)
    draw.ellipse(
        (head_cx - head_r, head_cy - head_r, head_cx + head_r, head_cy + head_r),
        fill=tuple(mix(accent, (60, 30, 30), 0.55)) + (255,),
    )

    # Hijab band — soft arc above the trapezoid.
    band_top = head_cy + int(head_r * 0.4)
    band_bot = int(H * QUAD[0][1]) - 8
    draw.polygon(
        [
            (W * 0.30,  band_top),
            (W * 0.70,  band_top),
            (W * QUAD[1][0] + 8, band_bot),
            (W * QUAD[0][0] - 8, band_bot),
        ],
        fill=tuple(mix(accent2, (255, 255, 255), 0.20)),
    )

    # Toub trapezoid — the warp target.
    corners = tuple((x * W, y * H) for (x, y) in QUAD)
    draw.polygon(list(corners), fill=accent + (255,))

    # Subtle fold lines on the toub (3 vertical curves).
    for i in (0.40, 0.50, 0.60):
        x_top = W * (QUAD[0][0] + (QUAD[1][0] - QUAD[0][0]) * i)
        x_bot = W * (QUAD[3][0] + (QUAD[2][0] - QUAD[3][0]) * i)
        draw.line(
            [(x_top, corners[0][1] + 12), (x_bot, corners[3][1] - 12)],
            fill=tuple(mix(accent, (0, 0, 0), 0.18)),
            width=2,
        )

    # Pattern band near the hem (suggests embroidery/raqma).
    hem_y = corners[3][1] - int(H * 0.05)
    draw.line(
        [(corners[3][0] + 30, hem_y), (corners[2][0] - 30, hem_y)],
        fill=accent2,
        width=4,
    )
    return corners


def draw_text_centered(
    draw: ImageDraw.ImageDraw,
    text: str,
    font: ImageFont.FreeTypeFont,
    y: int,
    fill: tuple[int, int, int],
) -> None:
    shaped = reshape(text)
    bbox = draw.textbbox((0, 0), shaped, font=font)
    tw = bbox[2] - bbox[0]
    draw.text(((W - tw) // 2 - bbox[0], y), shaped, font=font, fill=fill)


# ─────────────────────────────────────────────────────────────────
# Per-style rendering
# ─────────────────────────────────────────────────────────────────
def render_one(style: ToubStyle) -> dict:
    accent = hex_to_rgb(style.accent_hex)
    accent2 = hex_to_rgb(style.accent2_hex)

    # Backdrop: gradient from a darkened accent at top to deep night at bottom.
    top = mix(accent, (10, 5, 16), 0.55)
    bottom = (10, 5, 16)  # MawaaiColors.DeepNight
    img = Image.new("RGB", (W, H), bottom)
    draw_gradient(img, top, bottom)

    # Subtle vignette frame for premium feel.
    frame = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    fd = ImageDraw.Draw(frame)
    margin = 28
    fd.rectangle(
        (margin, margin, W - margin, H - margin),
        outline=(212, 175, 55, 90),  # ChampagneGold @ ~35% alpha
        width=3,
    )
    img.paste(frame, (0, 0), frame)

    draw = ImageDraw.Draw(img, "RGBA")
    draw_silhouette(draw, accent, accent2)

    # All Arabic text uses Amiri — its glyph table includes the Arabic
    # Presentation Forms-B block (FE70..FEFF) that `arabic_reshaper`
    # emits. Cairo only ships joined glyphs via OpenType GSUB, which
    # PIL's FreeType binding does not apply, so Cairo would render
    # disjoint letters here.
    title_font = load_font("amiri_bold.ttf", 84)
    draw_text_centered(draw, style.title_ar, title_font, int(H * 0.855), (255, 240, 245))

    brand_font = load_font("amiri_bold.ttf", 42)
    draw_text_centered(draw, BRAND_AR, brand_font, 56, (212, 175, 55))

    foot_font = load_font("amiri_regular.ttf", 28)
    draw_text_centered(draw, FOOTER_AR, foot_font, H - 60, (180, 130, 155))

    out_path = OUT_DIR / f"{style.file_id}.jpg"
    img.save(out_path, "JPEG", quality=85, optimize=True)
    print(f"  ✓ {out_path.relative_to(REPO)}  ({out_path.stat().st_size // 1024} KB)")

    return {
        "id": style.file_id,
        "quad": [list(p) for p in QUAD],
        "blend": style.blend,
        "alpha": style.alpha,
    }


def write_templates_json(entries: list[dict]) -> None:
    payload = {
        "_doc": (
            "Per-template overrides for the Sudanese thob category. Each entry's id must "
            "match the filename (without extension) of a .jpg/.jpeg/.png in this folder. "
            "quad: [[x0,y0],[x1,y1],[x2,y2],[x3,y3]] in normalised [0..1] coordinates, "
            "ordered top-left, top-right, bottom-right, bottom-left. The current entries "
            "point at programmatically generated placeholders (see scripts/"
            "generate_thob_placeholders.py); replace the .jpg files with real model photos "
            "and the same quad will warp the user's artwork onto the new fabric."
        ),
        "templates": entries,
    }
    json_path = OUT_DIR / "templates.json"
    json_path.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"\n  ✓ {json_path.relative_to(REPO)}  ({len(entries)} entries)")


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    print(f"Generating {len(STYLES)} placeholders → {OUT_DIR.relative_to(REPO)}")
    entries = [render_one(s) for s in STYLES]
    write_templates_json(entries)
    print("\nDone.")


if __name__ == "__main__":
    main()
