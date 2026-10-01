package xyz.volcanobay.cabalist.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.core.CabalistParticles;

public record GlyphParticleOptions(char character, boolean rune, int color) implements ParticleOptions {
    public static final MapCodec<GlyphParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("character").forGetter(options -> (int) options.character()),
            Codec.BOOL.optionalFieldOf("rune", true).forGetter(GlyphParticleOptions::rune),
            Codec.INT.optionalFieldOf("color", 0xFFFFFFFF).forGetter(GlyphParticleOptions::color)
    ).apply(instance, (character, rune, color) -> new GlyphParticleOptions((char) character.intValue(), rune, color)));

    public static final StreamCodec<ByteBuf, GlyphParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, options -> (int) options.character(),
            ByteBufCodecs.BOOL, GlyphParticleOptions::rune,
            ByteBufCodecs.INT, GlyphParticleOptions::color,
            (character, rune, color) -> new GlyphParticleOptions((char) character.intValue(), rune, color));

    @Override
    public @NotNull ParticleType<?> getType() {
        return CabalistParticles.GLYPH.get();
    }
}
