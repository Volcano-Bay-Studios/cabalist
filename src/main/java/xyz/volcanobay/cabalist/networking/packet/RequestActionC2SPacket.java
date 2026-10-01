package xyz.volcanobay.cabalist.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;

import java.util.UUID;

public record RequestActionC2SPacket(UUID id, int action) implements CustomPacketPayload {
    public static final int OPEN = 0;
    public static final int CLOSE = 1;
    public static final int DISMISS = 2;

    public static final Type<RequestActionC2SPacket> TYPE = new Type<>(Cabalist.id("request_action"));
    public static final StreamCodec<ByteBuf, RequestActionC2SPacket> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RequestActionC2SPacket::id,
            ByteBufCodecs.VAR_INT, RequestActionC2SPacket::action,
            RequestActionC2SPacket::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
