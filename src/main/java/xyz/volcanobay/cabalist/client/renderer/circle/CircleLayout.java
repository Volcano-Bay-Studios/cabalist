package xyz.volcanobay.cabalist.client.renderer.circle;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import xyz.volcanobay.cabalist.client.renderer.glyph.Glyph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * How a spell's words are written around its circle, in texels. Rune rings from the outside in, one after another.
 */
public record CircleLayout(List<List<Glyph>> rings, int starPoints) {
    public static final int OUTER_RING = 2;
    public static final int RING_WIDTH = 11;
    public static final int GLYPH_SPACING = 10;
    public static final int MIN_RING_CENTER = 12;
    public static final int MAX_RINGS = 7;
    public static final int MIN_STAR_POINTS = 3;
    public static final int MAX_STAR_POINTS = 8;
    private static final int MAX_CACHED = 256;
    private static final Map<String, CircleLayout> CACHE = new Object2ObjectOpenHashMap<>();

    // The smallest radius, a entire ring at a time.
    public static int getRadiusFor(String words) {
        int length = normalize(words).length();
        int radius = OUTER_RING + RING_WIDTH / 2 + MIN_RING_CENTER + 1;
        for (int rings = 1; rings < MAX_RINGS; rings++, radius += RING_WIDTH) {
            int capacity = 0;
            for (int ring = 0; ring < rings; ring++) {
                capacity += (int) (Mth.TWO_PI * getRingCenter(radius, ring) / GLYPH_SPACING);
            }
            if (capacity >= length) {
                break;
            }
        }
        return radius;
    }

    public static float getRingCenter(int radiusTexels, int ring) {
        return radiusTexels - OUTER_RING - RING_WIDTH * ring - RING_WIDTH / 2f;
    }

    public static CircleLayout of(String words, int radiusTexels) {
        String key = radiusTexels + ":" + words;
        CircleLayout cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        if (CACHE.size() > MAX_CACHED) {
            CACHE.clear();
        }
        CircleLayout layout = build(words, radiusTexels);
        CACHE.put(key, layout);
        return layout;
    }

    private static CircleLayout build(String words, int radiusTexels) {
        String text = normalize(words);
        int wordCount = text.isEmpty() ? 0 : text.split(" ").length;
        int starPoints = Mth.clamp(wordCount, MIN_STAR_POINTS, MAX_STAR_POINTS);
        List<List<Glyph>> rings = new ArrayList<>();
        if (text.isEmpty()) {
            return new CircleLayout(rings, starPoints);
        }
        int cursor = 0;
        for (int ring = 0; ring < MAX_RINGS && getRingCenter(radiusTexels, ring) >= MIN_RING_CENTER; ring++) {
            int capacity = (int) (Mth.TWO_PI * getRingCenter(radiusTexels, ring) / GLYPH_SPACING);
            List<Glyph> glyphs = new ArrayList<>(capacity);
            boolean isLast = cursor + capacity >= text.length();
            while (glyphs.size() < capacity && (cursor < text.length() || isLast)) {
                // Past the end, the last ring loops back to the start with a gap between repeats.
                int index = cursor % (text.length() + 1);
                glyphs.add(Glyph.rune(index == text.length() ? ' ' : text.charAt(index)));
                cursor++;
            }
            rings.add(glyphs);
            if (isLast) {
                break;
            }
        }
        return new CircleLayout(rings, starPoints);
    }

    public static String normalize(String words) {
        StringBuilder text = new StringBuilder(words.length());
        boolean isSpace = true;
        for (int i = 0; i < words.length(); i++) {
            char character = words.charAt(i);
            if (Character.isLetter(character) && character < 128) {
                text.append(Character.toLowerCase(character));
                isSpace = false;
            } else if (!isSpace) {
                text.append(' ');
                isSpace = true;
            }
        }
        return text.toString().trim();
    }
}
