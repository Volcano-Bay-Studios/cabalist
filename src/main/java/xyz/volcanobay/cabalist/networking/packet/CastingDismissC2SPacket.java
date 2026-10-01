package xyz.volcanobay.cabalist.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;

public record CastingDismissC2SPacket(LookTargetC2SPacket target) implements CustomPacketPayload {
    public static final CastingDismissC2SPacket STOP = new CastingDismissC2SPacket(LookTargetC2SPacket.NOTHING);

    public static final Type<CastingDismissC2SPacket> TYPE = new Type<>(Cabalist.id("casting_dismiss"));
    public static final StreamCodec<ByteBuf, CastingDismissC2SPacket> CODEC = StreamCodec.composite(
            LookTargetC2SPacket.CODEC, CastingDismissC2SPacket::target,
            CastingDismissC2SPacket::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
