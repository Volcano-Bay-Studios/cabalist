package xyz.volcanobay.cabalist.networking.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;

import java.util.List;
import java.util.Optional;

public record InscribedSpellsS2CPacket(Optional<BlockPos> block, int entityId, List<HangingSpellsS2CPacket.Entry> spells) implements CustomPacketPayload {

    public static final Type<InscribedSpellsS2CPacket> TYPE = new Type<>(Cabalist.id("inscribed_spells"));
    private static final Codec<InscribedSpellsS2CPacket> DATA_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.optionalFieldOf("block").forGetter(InscribedSpellsS2CPacket::block),
            Codec.INT.optionalFieldOf("entity", -1).forGetter(InscribedSpellsS2CPacket::entityId),
            HangingSpellsS2CPacket.Entry.CODEC.listOf().fieldOf("spells").forGetter(InscribedSpellsS2CPacket::spells)
    ).apply(instance, InscribedSpellsS2CPacket::new));
    public static final StreamCodec<ByteBuf, InscribedSpellsS2CPacket> CODEC = ByteBufCodecs.fromCodec(DATA_CODEC);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
