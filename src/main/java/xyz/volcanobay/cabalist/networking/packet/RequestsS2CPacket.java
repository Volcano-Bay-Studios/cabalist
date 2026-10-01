package xyz.volcanobay.cabalist.networking.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;

import java.util.List;
import java.util.UUID;

// The request circles floating around a player.
public record RequestsS2CPacket(int entityId, List<Entry> requests) implements CustomPacketPayload {
    public static final Type<RequestsS2CPacket> TYPE = new Type<>(Cabalist.id("requests"));
    private static final Codec<RequestsS2CPacket> DATA_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("entity").forGetter(RequestsS2CPacket::entityId),
            Entry.CODEC.listOf().fieldOf("requests").forGetter(RequestsS2CPacket::requests)
    ).apply(instance, RequestsS2CPacket::new));
    public static final StreamCodec<ByteBuf, RequestsS2CPacket> CODEC = ByteBufCodecs.fromCodec(DATA_CODEC);

    public static final int ROLE_ANSWER = 0;
    public static final int ROLE_STATUS = 1;
    public static final int ROLE_RESULT = 2;
    public static final int ROLE_COOLDOWN = 3;
    public static final int ROLE_INFO = 4;

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // Answer circles wait on this player, status circles show the player's own request, and results show how one was decided.
    public record Entry(UUID id, int role, String key, List<String> args, int color, boolean isWorld, boolean isRead, int outcome, List<Party> parties, boolean isOpen, List<String> details) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Entry::id),
                Codec.INT.fieldOf("role").forGetter(Entry::role),
                Codec.STRING.fieldOf("key").forGetter(Entry::key),
                Codec.STRING.listOf().fieldOf("args").forGetter(Entry::args),
                Codec.INT.fieldOf("color").forGetter(Entry::color),
                Codec.BOOL.fieldOf("world").forGetter(Entry::isWorld),
                Codec.BOOL.fieldOf("read").forGetter(Entry::isRead),
                Codec.INT.fieldOf("outcome").forGetter(Entry::outcome),
                Party.CODEC.listOf().fieldOf("parties").forGetter(Entry::parties),
                Codec.BOOL.fieldOf("open").forGetter(Entry::isOpen),
                Codec.STRING.listOf().fieldOf("details").forGetter(Entry::details)
        ).apply(instance, Entry::new));
    }

    // status is a PartyStatus ordinal.
    public record Party(String name, int status, int color) {
        public static final Codec<Party> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(Party::name),
                Codec.INT.fieldOf("status").forGetter(Party::status),
                Codec.INT.fieldOf("color").forGetter(Party::color)
        ).apply(instance, Party::new));
    }
}
