package xyz.volcanobay.cabalist.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.system.render.Palette;

import java.util.List;

public record CastingStateS2CPacket(int entityId, int phase, List<String> words, String current, String spoken, Palette palette,
                                    float gathered, float capacity, float rate, boolean frozen, float charge, float aim, boolean isDismissing, Vec3 dismissAt, float dismiss,
                                    boolean isPushing, Vec3 pushAt, String answer, int verdict, int ackSeq) implements CustomPacketPayload {
    public static final Type<CastingStateS2CPacket> TYPE = new Type<>(Cabalist.id("casting_state"));
    public static final StreamCodec<FriendlyByteBuf, CastingStateS2CPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.entityId);
                buf.writeVarInt(packet.phase);
                buf.writeCollection(packet.words, FriendlyByteBuf::writeUtf);
                buf.writeUtf(packet.current);
                buf.writeUtf(packet.spoken);
                Palette.STREAM_CODEC.encode(buf, packet.palette);
                buf.writeFloat(packet.gathered);
                buf.writeFloat(packet.capacity);
                buf.writeFloat(packet.rate);
                buf.writeBoolean(packet.frozen);
                buf.writeFloat(packet.charge);
                buf.writeFloat(packet.aim);
                buf.writeBoolean(packet.isDismissing);
                buf.writeVec3(packet.dismissAt);
                buf.writeFloat(packet.dismiss);
                buf.writeBoolean(packet.isPushing);
                buf.writeVec3(packet.pushAt);
                buf.writeUtf(packet.answer);
                buf.writeVarInt(packet.verdict);
                buf.writeVarInt(packet.ackSeq);
            },
            buf -> new CastingStateS2CPacket(buf.readVarInt(), buf.readVarInt(), buf.readList(FriendlyByteBuf::readUtf), buf.readUtf(), buf.readUtf(),
                    Palette.STREAM_CODEC.decode(buf), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readVec3(), buf.readFloat(),
                    buf.readBoolean(), buf.readVec3(), buf.readUtf(), buf.readVarInt(), buf.readVarInt()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
