package xyz.volcanobay.cabalist.networking.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.system.barrier.Barrier;

import java.util.List;

public record BarriersS2CPacket(ResourceKey<Level> dimension, List<Barrier> barriers) implements CustomPacketPayload {
    public static final Type<BarriersS2CPacket> TYPE = new Type<>(Cabalist.id("barriers"));
    private static final Codec<BarriersS2CPacket> DATA_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(BarriersS2CPacket::dimension),
            Barrier.CODEC.listOf().fieldOf("barriers").forGetter(BarriersS2CPacket::barriers)
    ).apply(instance, BarriersS2CPacket::new));
    public static final StreamCodec<ByteBuf, BarriersS2CPacket> CODEC = ByteBufCodecs.fromCodec(DATA_CODEC);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
