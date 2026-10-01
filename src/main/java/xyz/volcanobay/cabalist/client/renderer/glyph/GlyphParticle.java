package xyz.volcanobay.cabalist.client.renderer.glyph;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.particle.GlyphParticleOptions;

public class GlyphParticle extends Particle {
    private static final float SIZE = Glyphs.CELL / 16f * 0.6f;
    private static final int BASE_LIFETIME = 25;
    private static final int LIFETIME_VARIATION = 15;

    private final Glyph glyph;
    private final float phase;
    private final int tint;
    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;

    protected GlyphParticle(ClientLevel level, double x, double y, double z, double offsetX, double offsetY, double offsetZ, GlyphParticleOptions options) {
        super(level, x + offsetX, y + offsetY, z + offsetZ);
        this.glyph = options.rune() ? Glyph.rune(options.character()) : Glyph.latin(options.character());
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.lifetime = BASE_LIFETIME + random.nextInt(LIFETIME_VARIATION);
        this.phase = random.nextFloat() * Mth.TWO_PI;
        this.hasPhysics = false;
        this.gravity = 0;
        this.tint = options.color();
        this.alpha = 0;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        float progress = (float) age / lifetime;
        float remaining = 1 - progress * progress;
        setPos(targetX + offsetX * remaining, targetY + offsetY * remaining, targetZ + offsetZ * remaining);
    }

    @Override
    public void render(@NotNull VertexConsumer consumer, @NotNull Camera camera, float partialTick) {
        float fade = Mth.sin(Mth.PI * Mth.clamp((age + partialTick) / lifetime, 0, 1));
        Vec3 at = new Vec3(Mth.lerp(partialTick, xo, x), Mth.lerp(partialTick, yo, y), Mth.lerp(partialTick, zo, z));
        GlyphRenderer.LOOSE.billboard(camera, glyph, at, SIZE, FastColor.ARGB32.color(Math.round(fade * 255), tint), phase);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return GlyphParticleRenderType.INSTANCE;
    }

    public static class Provider implements ParticleProvider<GlyphParticleOptions> {
        @Override
        public Particle createParticle(@NotNull GlyphParticleOptions options, @NotNull ClientLevel level, double x, double y, double z,
                                       double offsetX, double offsetY, double offsetZ) {
            return new GlyphParticle(level, x, y, z, offsetX, offsetY, offsetZ, options);
        }
    }
}
