"""Generate Android launcher icons from a source PNG."""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

SRC = Path(
    r"C:\Users\legion\.cursor\projects\c-Users-legion-AndroidStudioProjects-warmbridge-demo\assets"
    r"\c__Users_legion_AppData_Roaming_Cursor_User_workspaceStorage_86dffc44f827a8f3edc6712736cdc590_images_"
    r"637086d5-7d38-4482-ae6b-5c40c0fb9774-a4b8f1b9-4cd2-4b89-8ae5-8f159294fc88.png"
)
RES = Path(__file__).resolve().parents[1] / "android" / "app" / "src" / "main" / "res"

# Adaptive icon canvas is 108dp; only the center 66dp circle is fully safe from masking.
# Scale artwork to ~56% so the full illustration stays visible on circle/squircle launchers.
ADAPTIVE_CONTENT_RATIO = 0.56
# Legacy square mipmaps can use more of the canvas; round launchers still clip slightly.
LEGACY_CONTENT_RATIO = 0.82

MIPMAP_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

BG = (255, 255, 255, 255)


def fit_centered(
    img: Image.Image,
    canvas_size: int,
    content_ratio: float,
    background: tuple[int, int, int, int] = BG,
) -> Image.Image:
    icon_size = max(1, round(canvas_size * content_ratio))
    scaled = img.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (canvas_size, canvas_size), background)
    offset = (canvas_size - icon_size) // 2
    canvas.paste(scaled, (offset, offset), scaled)
    return canvas


def save_icon(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, "PNG", optimize=True)
    print(f"Wrote {path} ({img.width}x{img.height})")


def main() -> None:
    source_path = Path(sys.argv[1]) if len(sys.argv) > 1 else SRC
    source = Image.open(source_path).convert("RGBA")

    save_icon(
        fit_centered(source, 432, ADAPTIVE_CONTENT_RATIO),
        RES / "drawable" / "ic_launcher_foreground.png",
    )

    for folder, size in MIPMAP_SIZES.items():
        base = RES / folder
        legacy = fit_centered(source, size, LEGACY_CONTENT_RATIO)
        save_icon(legacy, base / "ic_launcher.png")
        save_icon(legacy, base / "ic_launcher_round.png")

    print("Done.")


if __name__ == "__main__":
    main()
