package xyz.volcanobay.cabalist.system.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * How an aspect looks: Quasar emitters, shaped by the form, and layers drawn by the form's renderer.
 * The glyph color tints the spell's runes, circles and glyph particles; without one they take the first layer's color.
 */
public record RenderSpec(List<ResourceLocation> particles, List<RenderLayer> layers, int glyphColor, boolean barrier) {
    public static final int NO_COLOR = 0;
    public static final RenderSpec EMPTY = new RenderSpec(List.of(), List.of());

    public static final MapCodec<RenderSpec> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("particles", List.of()).forGetter(RenderSpec::particles),
            RenderLayer.CODEC.listOf().optionalFieldOf("layers", List.of()).forGetter(RenderSpec::layers),
            ColorCodec.CODEC.optionalFieldOf("glyph_color", NO_COLOR).forGetter(RenderSpec::glyphColor),
            Codec.BOOL.optionalFieldOf("barrier", false).forGetter(RenderSpec::barrier)
    ).apply(instance, RenderSpec::new));
    public static final Codec<RenderSpec> CODEC = MAP_CODEC.codec();

    public RenderSpec(List<ResourceLocation> particles, List<RenderLayer> layers, int glyphColor) {
        this(particles, layers, glyphColor, false);
    }

    public RenderSpec(List<ResourceLocation> particles, List<RenderLayer> layers) {
        this(particles, layers, NO_COLOR);
    }

    public RenderSpec(int glyphColor) {
        this(List.of(), List.of(), glyphColor);
    }

    public static final int DEFAULT_TINT = 0xFFB38CFF;

    public static int tintOf(List<RenderSpec> specs) {
        for (RenderSpec spec : specs) {
            if (spec.glyphColor() != NO_COLOR) {
                return 0xFF000000 | spec.glyphColor();
            }
        }
        for (RenderSpec spec : specs) {
            for (RenderLayer layer : spec.layers()) {
                return 0xFF000000 | layer.color();
            }
        }
        return DEFAULT_TINT;
    }

    public boolean isEmpty() {
        return particles.isEmpty() && layers.isEmpty() && !barrier;
    }
}
