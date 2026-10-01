package xyz.volcanobay.cabalist.system.blood;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * Whose blood an item is covered in. Blood given with a sacrificial dagger is consented.
 */
public record BloodStain(UUID owner, boolean consented) {
    public static final Codec<BloodStain> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(BloodStain::owner),
            Codec.BOOL.optionalFieldOf("consented", false).forGetter(BloodStain::consented)
    ).apply(instance, BloodStain::new));

    public static final StreamCodec<ByteBuf, BloodStain> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, BloodStain::owner,
            ByteBufCodecs.BOOL, BloodStain::consented,
            BloodStain::new);
}
