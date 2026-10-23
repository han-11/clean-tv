package dev.cleanmytv.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public final class WallpaperCatalogTest {
    @Test public void keepsOriginalsThenAddsNewPhotosInNumericOrder() {
        assertArrayEquals(new String[]{"wall-1.jpg", "wall-7.jpg", "wp-1.jpg", "wp-2.jpg", "wp-10.jpg"},
            WallpaperCatalog.find(new String[]{"wp-10.jpg", "wall-7.jpg", "wp-2.jpg", "wall-1.jpg", "wp-1.jpg"}));
    }
    @Test public void acceptsAllSeventyOneWithoutAHardCodedLimit() {
        String[] files = new String[71];
        for (int i = 0; i < 7; i++) files[i] = "wall-" + (i + 1) + ".jpg";
        for (int i = 0; i < 64; i++) files[i + 7] = "wp-" + (i + 1) + ".jpg";
        String[] result = WallpaperCatalog.find(files);
        assertEquals(71, result.length);
        assertEquals("wall-7.jpg", result[6]);
        assertEquals("wp-64.jpg", result[70]);
    }
    @Test public void ignoresLogosDirectoriesAndBackupFiles() {
        assertArrayEquals(new String[]{"wp-1.jpg"}, WallpaperCatalog.find(new String[]{
            "tv_logo.jpg", "wallpapers", "wallpaper.jpg", "wp-1.jpg.before", "wp-0.jpg", "wp-1.jpg"}));
    }
    @Test public void emptyAndNullCatalogsAreSafe() {
        assertEquals(0, WallpaperCatalog.find(null).length);
        assertEquals(0, WallpaperCatalog.find(new String[0]).length);
    }
    @Test public void filtersDuplicatesAndSupportsImageExtensions() {
        assertArrayEquals(new String[]{"wp-1.png", "wp-2.webp", "wp-3.jpeg"},
            WallpaperCatalog.find(new String[]{"wp-2.webp", "wp-1.png", "wp-1.png", null, "wp-3.jpeg"}));
    }
    @Test public void largeNumbersDoNotOverflowSorting() {
        assertArrayEquals(new String[]{"wp-2.jpg", "wp-99999999999999999999.jpg"},
            WallpaperCatalog.find(new String[]{"wp-99999999999999999999.jpg", "wp-2.jpg"}));
    }
}
