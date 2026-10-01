package xyz.volcanobay.cabalist.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;

public record CastingEndS2CPacket(int entityId, boolean cast) implements CustomPacketPayload {
    public static final Type<CastingEndS2CPacket> TYPE = new Type<>(Cabalist.id("casting_end"));
    public static final StreamCodec<ByteBuf, CastingEndS2CPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CastingEndS2CPacket::entityId,
            ByteBufCodecs.BOOL, CastingEndS2CPacket::cast,
            CastingEndS2CPacket::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
