package xyz.volcanobay.cabalist.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;

import java.util.List;

public record SpellEnergyS2CPacket(List<Entry> entries) implements CustomPacketPayload {

    public static final Type<SpellEnergyS2CPacket> TYPE = new Type<>(Cabalist.id("spell_energy"));
    public static final StreamCodec<ByteBuf, SpellEnergyS2CPacket> CODEC = Entry.CODEC.apply(ByteBufCodecs.list())
            .map(SpellEnergyS2CPacket::new, SpellEnergyS2CPacket::entries);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(int visualId, float flow, float remaining) {
        public static final StreamCodec<ByteBuf, Entry> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::visualId,
                ByteBufCodecs.FLOAT, Entry::flow,
                ByteBufCodecs.FLOAT, Entry::remaining,
                Entry::new);
    }
}
