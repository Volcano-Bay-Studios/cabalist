package xyz.volcanobay.cabalist.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;

public record SpellShakeS2CPacket(Vec3 at, float energy) implements CustomPacketPayload {
    public static final double RANGE = 48;

    public static final Type<SpellShakeS2CPacket> TYPE = new Type<>(Cabalist.id("spell_shake"));
    public static final StreamCodec<ByteBuf, SpellShakeS2CPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(Vec3.CODEC), SpellShakeS2CPacket::at,
            ByteBufCodecs.FLOAT, SpellShakeS2CPacket::energy,
            SpellShakeS2CPacket::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
