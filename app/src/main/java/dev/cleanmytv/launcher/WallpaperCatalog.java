package dev.cleanmytv.launcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Discovers wallpaper assets without including unrelated assets or changing their names. */
final class WallpaperCatalog {
    private static final Pattern NAME = Pattern.compile("^(wall|wp)-([1-9][0-9]*)\\.(jpg|jpeg|png|webp)$", Pattern.CASE_INSENSITIVE);

    static String[] find(String[] names) {
        if (names == null) return new String[0];
        ArrayList<String> result = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (String name : names) {
            if (name != null && NAME.matcher(name).matches() && seen.add(name)) result.add(name);
        }
        result.sort((left, right) -> {
            Matcher a = NAME.matcher(left), b = NAME.matcher(right);
            a.matches(); b.matches();
            int group = a.group(1).compareToIgnoreCase(b.group(1));
            if (group != 0) return group;
            String an = a.group(2), bn = b.group(2);
            int length = Integer.compare(an.length(), bn.length());
            if (length != 0) return length;
            int number = an.compareTo(bn);
            return number != 0 ? number : left.compareTo(right);
        });
        return result.toArray(new String[0]);
    }
}
