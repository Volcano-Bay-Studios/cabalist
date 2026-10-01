package xyz.volcanobay.cabalist.system.request;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import xyz.volcanobay.cabalist.system.render.ColorCodec;

import java.util.Optional;

/**
 * Colors for requests. This is data driven
 */
public record RequestStyle(int color, Optional<Integer> worldColor) {
    public static final RequestStyle DEFAULT = new RequestStyle(0xFFFFFF, Optional.empty());

    private static final Codec<Integer> COLOR = ColorCodec.CODEC.xmap(color -> color & 0xFFFFFF, color -> 0xFF000000 | color);
    public static final Codec<RequestStyle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            COLOR.fieldOf("color").forGetter(RequestStyle::color),
            COLOR.optionalFieldOf("world_color").forGetter(RequestStyle::worldColor)
    ).apply(instance, RequestStyle::new));

    public int getColor(boolean isWorld) {
        return isWorld ? worldColor.orElse(color) : color;
    }
}
