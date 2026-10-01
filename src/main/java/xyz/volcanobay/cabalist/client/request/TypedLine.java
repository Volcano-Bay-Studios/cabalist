package xyz.volcanobay.cabalist.client.request;

import foundry.veil.api.client.render.MatrixStack;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import xyz.volcanobay.cabalist.client.renderer.glyph.Glyph;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphRenderer;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphStyle;
import xyz.volcanobay.cabalist.client.renderer.glyph.Glyphs;
import xyz.volcanobay.cabalist.util.ColorHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TypedLine {
    private static final float SPEED = 0.25f;
    private static final float ARRIVED = 0.02f;
    private static final float BOB_SPEED = 0.12f;
    private static final float BOB_SPREAD = 0.7f;
    private static final float BOB_HEIGHT = 0.012f;
    private static final float GRAVITY = 0.004f;
    private static final float FALL_FADE = 1 / 24f;
    private static final int RED = 0xFF3A2A;

    private final RandomSource random = RandomSource.create();
    private final List<TypedChar> chars = new ArrayList<>();
    private final List<TypedChar> falling = new ArrayList<>();
    private String text = "";
    private int age;

    public void setText(String newText) {
        int keep = 0;
        while (keep < text.length() && keep < newText.length() && text.charAt(keep) == newText.charAt(keep)) {
            keep++;
        }
        while (chars.size() > keep) {
            TypedChar removed = chars.remove(chars.size() - 1);
            removed.vx = (random.nextFloat() - 0.5f) * 0.01f;
            removed.vy = random.nextFloat() * 0.015f;
            falling.add(removed);
        }
        for (int i = keep; i < newText.length(); i++) {
            chars.add(new TypedChar(newText.charAt(i)));
        }
        text = newText;
    }

    public void tick(float y) {
        age++;
        float[] slots = Glyphs.layout(text, ReadingScene.CELL);
        for (int i = 0; i < chars.size(); i++) {
            TypedChar typed = chars.get(i);
            typed.remember();
            typed.x = Mth.lerp(SPEED, typed.x, slots[i]);
            typed.y = Mth.lerp(SPEED, typed.y, y);
            typed.alpha = Math.min(1, typed.alpha + 0.15f);
            if (Math.abs(typed.x - slots[i]) + Math.abs(typed.y - y) < ARRIVED) {
                typed.isLatin = true;
            }
        }
        for (Iterator<TypedChar> iterator = falling.iterator(); iterator.hasNext(); ) {
            TypedChar typed = iterator.next();
            typed.remember();
            typed.vy -= GRAVITY;
            typed.x += typed.vx;
            typed.y += typed.vy;
            typed.alpha -= FALL_FADE;
            if (typed.alpha <= 0) {
                iterator.remove();
            }
        }
    }

    public void render(GlyphRenderer glyphs, MatrixStack pose, Vec3 center, Vec3 right, Vec3 up, int color, float opacity, float partialTick) {
        for (int i = 0; i < chars.size(); i++) {
            float bob = Mth.sin((age + partialTick) * BOB_SPEED + i * BOB_SPREAD) * BOB_HEIGHT;
            draw(glyphs, pose, chars.get(i), center, right, up, bob, color, opacity, partialTick);
        }
        for (TypedChar typed : falling) {
            draw(glyphs, pose, typed, center, right, up, 0, RED, opacity, partialTick);
        }
    }

    private static void draw(GlyphRenderer glyphs, MatrixStack pose, TypedChar typed, Vec3 center, Vec3 right, Vec3 up, float bob, int color, float opacity,
                             float partialTick) {
        float alpha = Mth.lerp(partialTick, typed.prevAlpha, typed.alpha) * opacity;
        if (alpha <= 0.01f) {
            return;
        }
        float x = Mth.lerp(partialTick, typed.prevX, typed.x);
        float y = Mth.lerp(partialTick, typed.prevY, typed.y) + bob;
        GlyphStyle style = typed.isLatin || !Character.isLetter(typed.character) ? GlyphStyle.LATIN : GlyphStyle.RUNE;
        glyphs.glyph(pose, new Glyph(style, typed.character), center.add(right.scale(x)).add(up.scale(y)), right, up, ReadingScene.CELL,
                ColorHelper.withAlpha(color, alpha), x / ReadingScene.CELL * GlyphRenderer.SPREAD);
    }

    private static class TypedChar {
        private final char character;
        private float x, y, prevX, prevY;
        private float vx, vy;
        private float alpha, prevAlpha;
        private boolean isLatin;

        private TypedChar(char character) {
            this.character = character;
        }

        private void remember() {
            prevX = x;
            prevY = y;
            prevAlpha = alpha;
        }
    }
}
