package xyz.volcanobay.cabalist.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;

public record SpellVisualEndS2CPacket(int id) implements CustomPacketPayload {

    public static final Type<SpellVisualEndS2CPacket> TYPE = new Type<>(Cabalist.id("spell_visual_end"));
    public static final StreamCodec<ByteBuf, SpellVisualEndS2CPacket> CODEC = ByteBufCodecs.VAR_INT.map(SpellVisualEndS2CPacket::new, SpellVisualEndS2CPacket::id);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
