package xyz.volcanobay.cabalist.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;

public record CastingInputC2SPacket(int action, int seq, String text) implements CustomPacketPayload {
    public static final int HOLD = 10;
    public static final int RELEASE = 11;
    public static final int CAST = 12;
    public static final int ABANDON = 13;
    public static final int PROMPT_ANSWER = 16;

    public static final Type<CastingInputC2SPacket> TYPE = new Type<>(Cabalist.id("casting_input"));
    public static final StreamCodec<FriendlyByteBuf, CastingInputC2SPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.action);
                buf.writeVarInt(packet.seq);
                buf.writeUtf(packet.text, 64);
            },
            buf -> new CastingInputC2SPacket(buf.readVarInt(), buf.readVarInt(), buf.readUtf(64)));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
