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

public record HiddenS2CPacket(List<Mark> entities, List<Mark> visuals, List<BlockMark> blocks, List<CircleMark> circles,
                              List<BlockCircleMark> blockCircles) implements CustomPacketPayload {
    public static final Type<HiddenS2CPacket> TYPE = new Type<>(Cabalist.id("hidden"));
    private static final Codec<HiddenS2CPacket> DATA_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Mark.CODEC.listOf().fieldOf("entities").forGetter(HiddenS2CPacket::entities),
            Mark.CODEC.listOf().fieldOf("visuals").forGetter(HiddenS2CPacket::visuals),
            BlockMark.CODEC.listOf().fieldOf("blocks").forGetter(HiddenS2CPacket::blocks),
            CircleMark.CODEC.listOf().fieldOf("circles").forGetter(HiddenS2CPacket::circles),
            BlockCircleMark.CODEC.listOf().fieldOf("block_circles").forGetter(HiddenS2CPacket::blockCircles)
    ).apply(instance, HiddenS2CPacket::new));
    public static final StreamCodec<ByteBuf, HiddenS2CPacket> CODEC = ByteBufCodecs.fromCodec(DATA_CODEC);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Mark(int id, float alpha) {
        public static final Codec<Mark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("id").forGetter(Mark::id),
                Codec.FLOAT.fieldOf("alpha").forGetter(Mark::alpha)
        ).apply(instance, Mark::new));
    }

    public record BlockMark(BlockPos pos, float alpha) {
        public static final Codec<BlockMark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(BlockMark::pos),
                Codec.FLOAT.fieldOf("alpha").forGetter(BlockMark::alpha)
        ).apply(instance, BlockMark::new));
    }

    public record CircleMark(int host, int wordsHash, float alpha) {
        public static final Codec<CircleMark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("host").forGetter(CircleMark::host),
                Codec.INT.fieldOf("words").forGetter(CircleMark::wordsHash),
                Codec.FLOAT.fieldOf("alpha").forGetter(CircleMark::alpha)
        ).apply(instance, CircleMark::new));
    }

    public record BlockCircleMark(BlockPos pos, int wordsHash, float alpha) {
        public static final Codec<BlockCircleMark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(BlockCircleMark::pos),
                Codec.INT.fieldOf("words").forGetter(BlockCircleMark::wordsHash),
                Codec.FLOAT.fieldOf("alpha").forGetter(BlockCircleMark::alpha)
        ).apply(instance, BlockCircleMark::new));
    }
}
