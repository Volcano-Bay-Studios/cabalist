package xyz.volcanobay.cabalist.util;

import net.minecraft.util.Mth;

/**
 * we are killing object allocations
 */
public final class ColorHelper {
    public static int withAlpha(int color, float alpha) {
        return Math.round(Mth.clamp(alpha, 0, 1) * 255) << 24 | color & 0xFFFFFF;
    }

    public static float getAlpha(int color) {
        return (color >>> 24) / 255f;
    }

    public static int lerp(int from, int to, float t) {
        int r = Math.round(Mth.lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int g = Math.round(Mth.lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int b = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return r << 16 | g << 8 | b;
    }

    public static int lighten(int color) {
        return lerp(color, 0xFFFFFF, 0.5f);
    }
}
