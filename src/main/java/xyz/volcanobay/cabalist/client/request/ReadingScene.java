package xyz.volcanobay.cabalist.client.request;

import foundry.veil.api.client.render.MatrixStack;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import xyz.volcanobay.cabalist.client.renderer.glyph.Glyph;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphRenderer;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphStyle;
import xyz.volcanobay.cabalist.client.renderer.glyph.Glyphs;
import xyz.volcanobay.cabalist.util.ColorHelper;

import java.util.ArrayList;
import java.util.List;

// Text projected up out of an opened request circle. glyphs only turn into Latin for the circle's owner
public class ReadingScene {
    public static final float CELL = 0.08f;
    public static final float LINE_HEIGHT = 0.11f;
    private static final float SPEED = 0.2f;
    private static final float ARRIVED = 0.02f;
    private static final float TICKS_PER_CHAR = 0.4f;
    private static final float FADE_PER_TICK = 0.1f;
    private static final float TEXT_BASE = 0.35f;
    private static final float BOB_SPEED = 0.12f;
    private static final float BOB_SPREAD = 8;
    private static final float BOB_HEIGHT = 0.012f;
    private static final int FLICKER_TICKS = 6;
    private static final String FLICKER_LETTERS = "abcdefghijklmnopqrstuvwxyz";

    private final List<SceneChar> chars = new ArrayList<>();
    private final List<List<SceneChar>> rows = new ArrayList<>();
    private List<Line> lines = List.of();
    private boolean isClosing;
    private int age;

    public record Line(String text, int color) {
    }

    public void setLines(List<Line> newLines) {
        if (newLines.equals(lines)) {
            return;
        }
        boolean isOpening = lines.isEmpty();
        List<List<SceneChar>> newRows = new ArrayList<>();
        chars.clear();
        int spawned = 0;
        for (int row = 0; row < newLines.size(); row++) {
            Line line = newLines.get(row);
            List<SceneChar> oldRow = row < rows.size() ? rows.get(row) : List.of();
            List<SceneChar> newRow = new ArrayList<>();
            float[] slots = Glyphs.layout(line.text(), CELL);
            float y = TEXT_BASE + (newLines.size() - 1 - row) * LINE_HEIGHT;
            for (int i = 0; i < line.text().length(); i++) {
                char character = line.text().charAt(i);
                if (character == ' ') {
                    newRow.add(null);
                    continue;
                }
                SceneChar sceneChar = i < oldRow.size() ? oldRow.get(i) : null;
                if (sceneChar == null) {
                    sceneChar = new SceneChar(character, isOpening ? age + spawned++ * TICKS_PER_CHAR : age);
                    if (!isOpening) {
                        sceneChar.place(slots[i], y);
                        sceneChar.flickerUntil = age + FLICKER_TICKS;
                    }
                } else {
                    if (sceneChar.character != character) {
                        sceneChar.character = character;
                        sceneChar.flickerUntil = age + FLICKER_TICKS;
                    }
                    if (sceneChar.hasArrived()) {
                        sceneChar.place(slots[i], y);
                    }
                }
                sceneChar.targetX = slots[i];
                sceneChar.targetY = y;
                sceneChar.color = line.color();
                newRow.add(sceneChar);
                chars.add(sceneChar);
            }
            newRows.add(newRow);
        }
        rows.clear();
        rows.addAll(newRows);
        lines = List.copyOf(newLines);
        isClosing = false;
    }

    public void close() {
        isClosing = true;
    }

    public boolean isFinished() {
        return isClosing && chars.stream().allMatch(sceneChar -> sceneChar.alpha <= 0);
    }

    public void tick(boolean isReader) {
        age++;
        for (SceneChar sceneChar : chars) {
            sceneChar.prevX = sceneChar.x;
            sceneChar.prevY = sceneChar.y;
            sceneChar.prevAlpha = sceneChar.alpha;
            if (age < sceneChar.startAt) {
                continue;
            }
            if (isClosing) {
                sceneChar.x = Mth.lerp(SPEED, sceneChar.x, 0);
                sceneChar.y = Mth.lerp(SPEED, sceneChar.y, 0);
                sceneChar.alpha = Math.max(0, sceneChar.alpha - FADE_PER_TICK);
                sceneChar.isLatin = false;
                continue;
            }
            sceneChar.x = Mth.lerp(SPEED, sceneChar.x, sceneChar.targetX);
            sceneChar.y = Mth.lerp(SPEED, sceneChar.y, sceneChar.targetY);
            sceneChar.alpha = Math.min(1, sceneChar.alpha + FADE_PER_TICK);
            if (isReader && Math.abs(sceneChar.x - sceneChar.targetX) + Math.abs(sceneChar.y - sceneChar.targetY) < ARRIVED) {
                sceneChar.isLatin = true;
            }
        }
    }

    public void render(GlyphRenderer glyphs, MatrixStack pose, Vec3 circle, Vec3 right, Vec3 up, float partialTick, float opacity) {
        for (SceneChar sceneChar : chars) {
            float alpha = Mth.lerp(partialTick, sceneChar.prevAlpha, sceneChar.alpha) * opacity;
            if (alpha <= 0.01f) {
                continue;
            }
            float x = Mth.lerp(partialTick, sceneChar.prevX, sceneChar.x);
            float y = Mth.lerp(partialTick, sceneChar.prevY, sceneChar.y) + Mth.sin((age + partialTick) * BOB_SPEED + x * BOB_SPREAD) * BOB_HEIGHT;
            Vec3 at = circle.add(right.scale(x)).add(up.scale(y));
            char shown = sceneChar.character;
            if (age < sceneChar.flickerUntil) {
                shown = FLICKER_LETTERS.charAt(Math.floorMod((int) (x * 1000) * 31 + age * 7, FLICKER_LETTERS.length()));
            }
            Glyph glyph = new Glyph(sceneChar.isLatin || !Character.isLetter(shown) ? GlyphStyle.LATIN : GlyphStyle.RUNE, shown);
            glyphs.glyph(pose, glyph, at, right, up, CELL, ColorHelper.withAlpha(sceneChar.color, alpha), x / CELL * GlyphRenderer.SPREAD);
        }
    }

    private static class SceneChar {
        private char character;
        private final float startAt;
        private int flickerUntil;
        private float x, y, prevX, prevY;
        private float targetX, targetY;
        private float alpha, prevAlpha;
        private int color;
        private boolean isLatin;

        private SceneChar(char character, float startAt) {
            this.character = character;
            this.startAt = startAt;
        }

        private boolean hasArrived() {
            return alpha >= 1 && Math.abs(x - targetX) + Math.abs(y - targetY) < ARRIVED;
        }

        private void place(float slotX, float slotY) {
            x = prevX = slotX;
            y = prevY = slotY;
            alpha = prevAlpha = Math.max(alpha, 1);
        }
    }
}
