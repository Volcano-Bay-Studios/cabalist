package xyz.volcanobay.cabalist.client.renderer.glyph;

import java.util.ArrayList;
import java.util.List;

public record Glyph(GlyphStyle style, char character) {
    public static final int RUNE_COUNT = 26;

    public static Glyph rune(char character) {
        return new Glyph(GlyphStyle.RUNE, character);
    }

    public static Glyph latin(char character) {
        return new Glyph(GlyphStyle.LATIN, character);
    }

    public static Glyph runeOf(int index) {
        return rune((char) ('a' + Math.floorMod(index, RUNE_COUNT)));
    }

    public static List<Glyph> of(String text, GlyphStyle style) {
        List<Glyph> glyphs = new ArrayList<>(text.length());
        for (int i = 0; i < text.length(); i++) {
            glyphs.add(new Glyph(style, text.charAt(i)));
        }
        return glyphs;
    }

    public boolean isSpace() {
        return Character.isWhitespace(character);
    }
}
