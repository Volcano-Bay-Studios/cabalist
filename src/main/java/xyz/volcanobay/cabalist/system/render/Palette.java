package xyz.volcanobay.cabalist.system.render;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import java.util.List;

public record Palette(List<Integer> colors) {
    public static final Palette EMPTY = new Palette(List.of());
    public static final Codec<Palette> CODEC = Codec.INT.listOf().xmap(Palette::new, Palette::colors);
    public static final StreamCodec<ByteBuf, Palette> STREAM_CODEC = ByteBufCodecs.INT.apply(ByteBufCodecs.list()).map(Palette::new, Palette::colors);
    private static final float FLOW_PER_TICK = 0.01f;
    private static final int VOID_BRIGHTNESS = 48;

    public static Palette of(int color) {
        return new Palette(List.of(0xFF000000 | color));
    }

    public static boolean isVoid(int color) {
        return Math.max(color >> 16 & 0xFF, Math.max(color >> 8 & 0xFF, color & 0xFF)) < VOID_BRIGHTNESS;
    }

    public boolean hasVoid() {
        for (int color : colors) {
            if (isVoid(color)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return colors.isEmpty();
    }

    public Palette or(Palette fallback) {
        return isEmpty() ? fallback : this;
    }

    public int getPrimary() {
        return colors.isEmpty() ? RenderSpec.DEFAULT_TINT : colors.get(0);
    }

    public int sample(float position, float time) {
        if (colors.size() <= 1) {
            return getPrimary();
        }
        float along = (position + time * FLOW_PER_TICK) * colors.size();
        int from = Math.floorMod(Mth.floor(along), colors.size());
        float blend = along - Mth.floor(along);
        blend = blend * blend * (3 - 2 * blend);
        return FastColor.ARGB32.lerp(blend, colors.get(from), colors.get((from + 1) % colors.size()));
    }
}
