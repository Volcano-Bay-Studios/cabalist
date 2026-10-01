package xyz.volcanobay.cabalist.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;

import java.util.UUID;

// The magic circle under a caster's crosshair as only the client knows where circles are drawn.
public record LookTargetC2SPacket(int kind, int hostId, UUID requestId, int role, int index, int wordsHash, Vec3 at, float distance)
        implements CustomPacketPayload {
    public static final int NONE = 0;
    public static final int REQUEST = 1;
    public static final int SPELL = 2;
    public static final int DRAFT = 3;
    public static final int FORM = 4;
    public static final LookTargetC2SPacket NOTHING = new LookTargetC2SPacket(NONE, -1, new UUID(0, 0), 0, 0, 0, Vec3.ZERO, 0);

    public static final Type<LookTargetC2SPacket> TYPE = new Type<>(Cabalist.id("look_target"));
    public static final StreamCodec<ByteBuf, LookTargetC2SPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                ByteBufCodecs.VAR_INT.encode(buf, packet.kind);
                ByteBufCodecs.VAR_INT.encode(buf, packet.hostId);
                UUIDUtil.STREAM_CODEC.encode(buf, packet.requestId);
                ByteBufCodecs.VAR_INT.encode(buf, packet.role);
                ByteBufCodecs.VAR_INT.encode(buf, packet.index);
                ByteBufCodecs.INT.encode(buf, packet.wordsHash);
                ByteBufCodecs.DOUBLE.encode(buf, packet.at.x);
                ByteBufCodecs.DOUBLE.encode(buf, packet.at.y);
                ByteBufCodecs.DOUBLE.encode(buf, packet.at.z);
                ByteBufCodecs.FLOAT.encode(buf, packet.distance);
            },
            buf -> new LookTargetC2SPacket(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), UUIDUtil.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.INT.decode(buf),
                    new Vec3(ByteBufCodecs.DOUBLE.decode(buf), ByteBufCodecs.DOUBLE.decode(buf), ByteBufCodecs.DOUBLE.decode(buf)),
                    ByteBufCodecs.FLOAT.decode(buf)));

    public boolean isSameTarget(LookTargetC2SPacket other) {
        return kind == other.kind && hostId == other.hostId && requestId.equals(other.requestId) && role == other.role
                && index == other.index && wordsHash == other.wordsHash;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
