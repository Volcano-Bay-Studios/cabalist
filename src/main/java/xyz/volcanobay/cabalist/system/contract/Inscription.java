package xyz.volcanobay.cabalist.system.contract;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record Inscription(List<Line> lines) {
    public static final Codec<Inscription> CODEC = Line.CODEC.listOf().xmap(Inscription::new, Inscription::lines);
    public static final StreamCodec<ByteBuf, Inscription> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public boolean hasTexts(List<String> texts) {
        if (texts.size() != lines.size()) {
            return false;
        }
        for (int i = 0; i < texts.size(); i++) {
            if (!texts.get(i).equals(lines.get(i).text())) {
                return false;
            }
        }
        return true;
    }

    public record Line(String text, int tint) {
        public static final Codec<Line> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("text").forGetter(Line::text),
                Codec.INT.fieldOf("tint").forGetter(Line::tint)
        ).apply(instance, Line::new));
    }
}
