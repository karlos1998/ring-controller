"""Generate Android launcher icon density variants from the approved master."""

from pathlib import Path

from PIL import Image, ImageOps


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
MASTER = Path(__file__).with_name("d4wid-ring-icon-master.png")
MONOCHROME_MASTER = Path(__file__).with_name("d4wid-ring-icon-monochrome-master.png")

LEGACY_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

ADAPTIVE_SIZES = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}


def resized(image: Image.Image, size: int) -> Image.Image:
    return image.resize((size, size), Image.Resampling.LANCZOS)


def make_monochrome(master: Image.Image) -> Image.Image:
    grayscale = ImageOps.grayscale(master.convert("RGB"))
    alpha = grayscale.point(
        lambda value: 0 if value < 42 else min(255, round((value - 42) * 255 / 150)),
    )
    white = Image.new("RGBA", master.size, (255, 255, 255, 0))
    white.putalpha(alpha)
    return white


def main() -> None:
    master = Image.open(MASTER).convert("RGB")
    monochrome = make_monochrome(master)
    monochrome.save(MONOCHROME_MASTER, optimize=True)

    for density, size in LEGACY_SIZES.items():
        destination = RES / f"mipmap-{density}"
        destination.mkdir(parents=True, exist_ok=True)
        icon = resized(master, size)
        icon.save(destination / "ic_launcher.png", optimize=True)
        icon.save(destination / "ic_launcher_round.png", optimize=True)

    for density, size in ADAPTIVE_SIZES.items():
        destination = RES / f"drawable-{density}"
        destination.mkdir(parents=True, exist_ok=True)
        resized(master, size).save(destination / "ic_launcher_foreground.png", optimize=True)
        resized(monochrome, size).save(destination / "ic_launcher_monochrome.png", optimize=True)


if __name__ == "__main__":
    main()
