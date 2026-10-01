package xyz.volcanobay.cabalist.networking.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.system.render.FormShape;
import xyz.volcanobay.cabalist.system.render.Palette;
import xyz.volcanobay.cabalist.system.render.RenderSpec;

import java.util.List;

public record SpellVisualS2CPacket(int id, FormShape shape, List<RenderSpec> specs, String words, Palette palette) implements CustomPacketPayload {

    public static final Type<SpellVisualS2CPacket> TYPE = new Type<>(Cabalist.id("spell_visual"));
    private static final Codec<SpellVisualS2CPacket> DATA_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(SpellVisualS2CPacket::id),
            FormShape.CODEC.fieldOf("shape").forGetter(SpellVisualS2CPacket::shape),
            RenderSpec.CODEC.listOf().fieldOf("specs").forGetter(SpellVisualS2CPacket::specs),
            Codec.STRING.optionalFieldOf("words", "").forGetter(SpellVisualS2CPacket::words),
            Palette.CODEC.optionalFieldOf("palette", Palette.EMPTY).forGetter(SpellVisualS2CPacket::palette)
    ).apply(instance, SpellVisualS2CPacket::new));
    public static final StreamCodec<ByteBuf, SpellVisualS2CPacket> CODEC = ByteBufCodecs.fromCodec(DATA_CODEC);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
