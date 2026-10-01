package xyz.volcanobay.cabalist.system.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

/**
 * One layer drawn by a form's renderer: a Veil shader, a texture, and a tint. Bloom layers also glow.
 * Layers are additive by default so they mix with particles and each other regardless of draw order.
 */
public record RenderLayer(ResourceLocation shader, ResourceLocation texture, int color, boolean bloom, boolean additive) {
    public static final Codec<RenderLayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("shader").forGetter(RenderLayer::shader),
            ResourceLocation.CODEC.fieldOf("texture").forGetter(RenderLayer::texture),
            ColorCodec.CODEC.optionalFieldOf("color", 0xFFFFFFFF).forGetter(RenderLayer::color),
            Codec.BOOL.optionalFieldOf("bloom", false).forGetter(RenderLayer::bloom),
            Codec.BOOL.optionalFieldOf("additive", true).forGetter(RenderLayer::additive)
    ).apply(instance, RenderLayer::new));

    public RenderLayer(ResourceLocation shader, ResourceLocation texture, int color, boolean bloom) {
        this(shader, texture, color, bloom, true);
    }
}
