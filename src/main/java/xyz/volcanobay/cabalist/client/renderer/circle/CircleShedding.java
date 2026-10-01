package xyz.volcanobay.cabalist.client.renderer.circle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphRenderer;
import xyz.volcanobay.cabalist.particle.GlyphParticleOptions;
import xyz.volcanobay.cabalist.system.render.Palette;

// Circles can "shed" glyphs when they are spending.
public class CircleShedding {
    private static final float GLYPHS_PER_FLOW = 0.02f;
    private static final float MAX_PER_TICK = 4;
    private static final double DRIFT = 0.8;

    public static void shed(ClientLevel level, MagicCircle circle, float flow, Palette palette, RandomSource random) {
        float expected = Math.min(MAX_PER_TICK, flow * GLYPHS_PER_FLOW);
        int count = (int) expected + (random.nextFloat() < expected - (int) expected ? 1 : 0);
        if (count == 0) {
            return;
        }
        Vec3 normal = circle.normal();
        Vec3 right = circle.getRight();
        Vec3 up = circle.getUp();
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            Vec3 outward = right.scale(Mth.cos(angle)).add(up.scale(Mth.sin(angle)));
            // Glyph particles ease from their offset in to where they're placed, so they're placed past the rim and start on it.
            Vec3 drift = outward.add(normal.scale(0.5)).scale(DRIFT);
            Vec3 end = circle.center().add(outward.scale(circle.radius())).add(drift);
            char letter = (char) ('a' + random.nextInt(26));
            level.addParticle(new GlyphParticleOptions(letter, true, palette.sample(angle / Mth.TWO_PI, GlyphRenderer.getTime())), end.x, end.y, end.z, -drift.x, -drift.y, -drift.z);
        }
    }
}
