package net.glassmc.mapartcopyright.artwork;

import java.util.List;
import java.util.Locale;

/** Width by height, viewed from the front of a vertical wall. */
public record ArtworkSize(int width, int height) {
    public static final List<String> PRESETS = List.of("2x1", "2x2", "2x3", "3x3");

    public ArtworkSize {
        if (!((width == 2 && height >= 1 && height <= 3) || (width == 3 && height == 3)))
            throw new IllegalArgumentException("Choose 2x1, 2x2, 2x3, or 3x3 (width x height).");
    }

    public static ArtworkSize parse(String input) {
        String normalized = input.toLowerCase(Locale.ROOT).replace('×', 'x');
        if (!PRESETS.contains(normalized)) throw new IllegalArgumentException("Choose 2x1, 2x2, 2x3, or 3x3 (width x height).");
        return new ArtworkSize(normalized.charAt(0) - '0', normalized.charAt(2) - '0');
    }

    public int tiles() { return width * height; }
    @Override public String toString() { return width + "x" + height; }
}
