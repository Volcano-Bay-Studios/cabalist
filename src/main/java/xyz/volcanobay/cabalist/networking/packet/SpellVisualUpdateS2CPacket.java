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

public record SpellVisualUpdateS2CPacket(int id, FormShape shape) implements CustomPacketPayload {

    public static final Type<SpellVisualUpdateS2CPacket> TYPE = new Type<>(Cabalist.id("spell_visual_update"));
    private static final Codec<SpellVisualUpdateS2CPacket> DATA_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(SpellVisualUpdateS2CPacket::id),
            FormShape.CODEC.fieldOf("shape").forGetter(SpellVisualUpdateS2CPacket::shape)
    ).apply(instance, SpellVisualUpdateS2CPacket::new));
    public static final StreamCodec<ByteBuf, SpellVisualUpdateS2CPacket> CODEC = ByteBufCodecs.fromCodec(DATA_CODEC);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
