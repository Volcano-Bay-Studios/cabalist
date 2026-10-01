package xyz.volcanobay.cabalist.client.renderer.glyph;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import org.jetbrains.annotations.NotNull;

public final class GlyphParticleRenderType implements ParticleRenderType {
    public static final GlyphParticleRenderType INSTANCE = new GlyphParticleRenderType();

    private GlyphParticleRenderType() {
    }

    @Override
    public @NotNull BufferBuilder begin(@NotNull Tesselator tesselator, @NotNull TextureManager textureManager) {
        return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
    }

    @Override
    public boolean isTranslucent() {
        return true;
    }

    @Override
    public @NotNull String toString() {
        return "cabalist:glyph";
    }
}
