#!/usr/bin/env python3
"""Prepare bundled wallpapers on macOS, leaving originals untouched."""
import argparse
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

MANAGED = re.compile(r"^(wall|wp)-[1-9][0-9]*\.(jpg|jpeg|png|webp)$", re.I)
SOURCE = re.compile(r"^wp-([1-9][0-9]*)\.(jpg|jpeg|png|webp)$", re.I)


def run(source, count, replace, backup_root):
    root = Path(__file__).resolve().parent
    assets = root / "app/src/main/assets"
    if not (root / "app/src/main/AndroidManifest.xml").is_file():
        raise ValueError("Run this script from the clean-tv project.")
    if not source.is_dir():
        raise ValueError("Source wallpaper folder does not exist.")
    if source.resolve() == assets.resolve():
        raise ValueError("Use an original-photo folder outside the app assets directory.")
    if not shutil.which("sips"):
        raise ValueError("This importer needs macOS's built-in sips tool.")
    photos = {}
    for path in source.iterdir():
        match = SOURCE.fullmatch(path.name)
        if path.is_file() and match:
            number = int(match.group(1))
            if number in photos:
                raise ValueError(f"Two source photos have number {number}.")
            photos[number] = path
    if not photos:
        raise ValueError("No wp-N image files found.")
    if count is not None and set(photos) != set(range(1, count + 1)):
        raise ValueError(f"Expected exactly wp-1 through wp-{count}. No app files changed.")
    assets.mkdir(parents=True, exist_ok=True)
    old = [p for p in assets.iterdir() if p.is_file() and MANAGED.fullmatch(p.name)]
    if not replace and any((assets / f"wp-{n}.jpg").exists() for n in photos):
        raise ValueError("Some destination photos exist. Use --replace-set to archive and replace the full set.")
    with tempfile.TemporaryDirectory(prefix="clean-tv-wallpapers-") as temporary:
        stage = Path(temporary)
        for number, photo in sorted(photos.items()):
            destination = stage / f"wp-{number}.jpg"
            result = subprocess.run(["sips", "-Z", "1920", "-s", "format", "jpeg",
                str(photo), "--out", str(destination)], capture_output=True, text=True)
            if result.returncode != 0 or not destination.is_file() or destination.stat().st_size == 0:
                raise ValueError(f"Could not prepare {photo.name}. No app photos changed.")
            print(f"Prepared {number} ({photo.name})", flush=True)
        backup = None
        if replace and old:
            backup_root.mkdir(parents=True, exist_ok=True)
            backup = Path(tempfile.mkdtemp(prefix="clean-tv-wallpapers-backup-", dir=backup_root))
            for photo in old:
                shutil.copy2(photo, backup / photo.name)
            print(f"Previous wallpaper backup: {backup}", flush=True)
        try:
            if replace:
                for photo in old:
                    photo.unlink()
            for photo in stage.iterdir():
                shutil.copy2(photo, assets / photo.name)
        except OSError:
            for number in photos:
                (assets / f"wp-{number}.jpg").unlink(missing_ok=True)
            if backup is not None:
                for photo in backup.iterdir():
                    shutil.copy2(photo, assets / photo.name)
            raise
    print(f"Imported {len(photos)} photos. Originals unchanged. Rotation remains 60 seconds.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("--count", type=int, help="Require exactly wp-1 through wp-N")
    parser.add_argument("--replace-set", action="store_true", help="Back up and replace all existing wallpaper assets")
    parser.add_argument("--backup-root", type=Path, default=Path.home() / "Desktop")
    args = parser.parse_args()
    try:
        run(args.source.expanduser(), args.count, args.replace_set, args.backup_root.expanduser())
    except (ValueError, OSError) as failure:
        parser.exit(1, f"Wallpaper import stopped: {failure}\n")


if __name__ == "__main__":
    main()
