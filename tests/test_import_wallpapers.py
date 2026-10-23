import contextlib
import importlib.util
import io
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("wallpaper_importer", Path(__file__).resolve().parents[1] / "import-wallpapers.py")
importer = importlib.util.module_from_spec(spec)
spec.loader.exec_module(importer)


class ImportWallpapersTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.base = Path(self.temporary.name)
        self.root = self.base / "project"
        self.assets = self.root / "app/src/main/assets"
        self.assets.mkdir(parents=True)
        (self.assets.parent / "AndroidManifest.xml").write_text("manifest")
        self.photos = self.base / "originals"
        self.photos.mkdir()
        for number in range(1, 65):
            (self.photos / f"wp-{number}.jpg").write_bytes(f"original-{number}".encode())
        for number in range(1, 8):
            (self.assets / f"wall-{number}.jpg").write_bytes(f"old-{number}".encode())
        self.backups = self.base / "backups"
        self.addCleanup(patch.stopall)
        patch.object(importer, "__file__", str(self.root / "import-wallpapers.py")).start()
        patch.object(importer.shutil, "which", return_value="sips").start()
        self.convert = patch.object(importer.subprocess, "run", side_effect=self.fake_sips).start()

    def fake_sips(self, command, **kwargs):
        Path(command[-1]).write_bytes(Path(command[-3]).read_bytes())
        return subprocess.CompletedProcess(command, 0)

    def run_import(self):
        with contextlib.redirect_stdout(io.StringIO()):
            importer.run(self.photos, 64, True, self.backups)

    def test_replacement_preserves_originals_and_archives_seven(self):
        self.run_import()
        self.assertEqual(64, len(list(self.assets.iterdir())))
        archived = list(self.backups.iterdir())
        self.assertEqual(1, len(archived))
        self.assertEqual(7, len(list(archived[0].iterdir())))
        self.assertEqual(b"old-1", (archived[0] / "wall-1.jpg").read_bytes())
        self.assertEqual(b"original-64", (self.photos / "wp-64.jpg").read_bytes())
        self.assertEqual(b"original-64", (self.assets / "wp-64.jpg").read_bytes())

    def test_missing_photo_leaves_app_untouched(self):
        (self.photos / "wp-64.jpg").unlink()
        with self.assertRaisesRegex(ValueError, "Expected exactly"):
            self.run_import()
        self.convert.assert_not_called()
        self.assertEqual(7, len(list(self.assets.iterdir())))

    def test_failed_conversion_leaves_all_existing_photos(self):
        def fail_last(command, **kwargs):
            if Path(command[-3]).name == "wp-64.jpg":
                return subprocess.CompletedProcess(command, 1)
            return self.fake_sips(command, **kwargs)
        self.convert.side_effect = fail_last
        with self.assertRaisesRegex(ValueError, "Could not prepare"):
            self.run_import()
        self.assertEqual(7, len(list(self.assets.iterdir())))
        self.assertFalse(self.backups.exists())

    def test_copy_failure_restores_previous_set(self):
        real_copy = shutil.copy2
        def fail_second(source, destination):
            destination = Path(destination)
            if destination.parent == self.assets and destination.name == "wp-2.jpg":
                raise OSError("simulated copy failure")
            return real_copy(source, destination)
        with patch.object(importer.shutil, "copy2", side_effect=fail_second):
            with self.assertRaisesRegex(OSError, "simulated"):
                self.run_import()
        self.assertEqual(7, len(list(self.assets.iterdir())))
        self.assertEqual(b"old-7", (self.assets / "wall-7.jpg").read_bytes())


if __name__ == "__main__":
    unittest.main()
