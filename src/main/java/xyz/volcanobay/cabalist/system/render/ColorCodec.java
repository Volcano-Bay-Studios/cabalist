package xyz.volcanobay.cabalist.system.render;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;

public final class ColorCodec {
    public static final Codec<Integer> CODEC = Codec.either(Codec.INT, Codec.STRING.comapFlatMap(ColorCodec::parse, ColorCodec::format))
            .xmap(either -> either.map(color -> color, color -> color), Either::right);

    private static DataResult<Integer> parse(String text) {
        String hex = text.startsWith("#") ? text.substring(1) : text;
        if (hex.length() != 6 && hex.length() != 8) {
            return DataResult.error(() -> "Color must be #RRGGBB or #AARRGGBB: " + text);
        }
        try {
            int value = (int) Long.parseLong(hex, 16);
            return DataResult.success(hex.length() == 6 ? 0xFF000000 | value : value);
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Not a hex color: " + text);
        }
    }

    private static String format(int color) {
        return "#" + String.format(Locale.ROOT, "%08X", color);
    }
}
