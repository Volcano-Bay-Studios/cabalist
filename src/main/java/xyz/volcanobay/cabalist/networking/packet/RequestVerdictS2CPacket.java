package xyz.volcanobay.cabalist.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;

import java.util.UUID;

// What an answer typed into a request reads as
public record RequestVerdictS2CPacket(UUID id, String text, int verdict, boolean isSubmit) implements CustomPacketPayload {
    public static final Type<RequestVerdictS2CPacket> TYPE = new Type<>(Cabalist.id("request_verdict"));
    public static final StreamCodec<ByteBuf, RequestVerdictS2CPacket> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RequestVerdictS2CPacket::id,
            ByteBufCodecs.stringUtf8(64), RequestVerdictS2CPacket::text,
            ByteBufCodecs.VAR_INT, RequestVerdictS2CPacket::verdict,
            ByteBufCodecs.BOOL, RequestVerdictS2CPacket::isSubmit,
            RequestVerdictS2CPacket::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
