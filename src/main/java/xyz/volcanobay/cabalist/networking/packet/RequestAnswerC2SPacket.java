package xyz.volcanobay.cabalist.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;

import java.util.UUID;

public record RequestAnswerC2SPacket(UUID id, String text, boolean isSubmit) implements CustomPacketPayload {
    public static final Type<RequestAnswerC2SPacket> TYPE = new Type<>(Cabalist.id("request_answer"));
    public static final StreamCodec<ByteBuf, RequestAnswerC2SPacket> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RequestAnswerC2SPacket::id,
            ByteBufCodecs.stringUtf8(64), RequestAnswerC2SPacket::text,
            ByteBufCodecs.BOOL, RequestAnswerC2SPacket::isSubmit,
            RequestAnswerC2SPacket::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
