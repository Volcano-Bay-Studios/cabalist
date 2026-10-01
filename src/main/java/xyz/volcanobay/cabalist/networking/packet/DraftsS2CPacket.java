package xyz.volcanobay.cabalist.networking.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Contract drafts near the player, laid out around the entity or block holding the contract.
public record DraftsS2CPacket(List<Draft> drafts) implements CustomPacketPayload {
    public static final Type<DraftsS2CPacket> TYPE = new Type<>(Cabalist.id("drafts"));
    private static final Codec<DraftsS2CPacket> DATA_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Draft.CODEC.listOf().fieldOf("drafts").forGetter(DraftsS2CPacket::drafts)
    ).apply(instance, DraftsS2CPacket::new));
    public static final StreamCodec<ByteBuf, DraftsS2CPacket> CODEC = ByteBufCodecs.fromCodec(DATA_CODEC);

    public static final int KIND_NAME = 0;
    public static final int KIND_LINE = 1;
    public static final int KIND_ADD = 2;

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Draft(UUID id, UUID author, int anchorEntity, Optional<BlockPos> anchorBlock, List<Circle> circles) {
        public static final Codec<Draft> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Draft::id),
                UUIDUtil.CODEC.fieldOf("author").forGetter(Draft::author),
                Codec.INT.fieldOf("anchor_entity").forGetter(Draft::anchorEntity),
                BlockPos.CODEC.optionalFieldOf("anchor_block").forGetter(Draft::anchorBlock),
                Circle.CODEC.listOf().fieldOf("circles").forGetter(Draft::circles)
        ).apply(instance, Draft::new));
    }

    public record Circle(int id, int kind, String text, int color, boolean isLatin) {
        public static final Codec<Circle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("id").forGetter(Circle::id),
                Codec.INT.fieldOf("kind").forGetter(Circle::kind),
                Codec.STRING.fieldOf("text").forGetter(Circle::text),
                Codec.INT.fieldOf("color").forGetter(Circle::color),
                Codec.BOOL.fieldOf("latin").forGetter(Circle::isLatin)
        ).apply(instance, Circle::new));
    }
}
