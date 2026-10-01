package xyz.volcanobay.cabalist.system.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record AspectRenderSpec(ResourceLocation aspect, RenderSpec spec) {
    public static final Codec<AspectRenderSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("aspect").forGetter(AspectRenderSpec::aspect),
            RenderSpec.MAP_CODEC.forGetter(AspectRenderSpec::spec)
    ).apply(instance, AspectRenderSpec::new));
}
