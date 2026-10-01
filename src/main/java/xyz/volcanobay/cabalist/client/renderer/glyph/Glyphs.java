package xyz.volcanobay.cabalist.client.renderer.glyph;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public final class Glyphs {
    public static final int CELL = 8;
    public static final int SHEET_CELLS = 16;
    private static final int SPACE_ADVANCE = 4;
    private static final Map<Glyph, Integer> ADVANCES = new HashMap<>();

    public static boolean isDrawable(Glyph glyph) {
        char character = glyph.character();
        if (glyph.isSpace() || character >= SHEET_CELLS * SHEET_CELLS) {
            return false;
        }
        return glyph.style() != GlyphStyle.RUNE || Character.isLetter(character) && character < 128;
    }

    public static float u0(Glyph glyph) {
        return (glyph.character() % SHEET_CELLS) / (float) SHEET_CELLS;
    }

    public static float v0(Glyph glyph) {
        return (glyph.character() / SHEET_CELLS) / (float) SHEET_CELLS;
    }

    public static float cellSize() {
        return 1f / SHEET_CELLS;
    }

    public static int advance(Glyph glyph) {
        if (glyph.isSpace()) {
            return SPACE_ADVANCE;
        }
        return ADVANCES.computeIfAbsent(glyph, key -> Minecraft.getInstance().font.width(Component.literal(String.valueOf(key.character()))
                .withStyle(style -> style.withFont(key.style().getFont()))));
    }

    public static float[] layout(String text, float cellSize) {
        float[] slots = new float[text.length()];
        float width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += advance(Glyph.latin(text.charAt(i))) / (float) CELL * cellSize;
        }
        float x = -width / 2;
        for (int i = 0; i < text.length(); i++) {
            float advance = advance(Glyph.latin(text.charAt(i))) / (float) CELL * cellSize;
            slots[i] = x + advance / 2;
            x += advance;
        }
        return slots;
    }

    public static float getCenterShift(Glyph glyph) {
        return (CELL - Math.min(CELL, advance(glyph))) / 2f / CELL;
    }
}
