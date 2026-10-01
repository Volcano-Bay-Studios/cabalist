package xyz.volcanobay.cabalist.networking.packet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.system.render.Palette;
import xyz.volcanobay.cabalist.system.spell.PendingSpell;
import xyz.volcanobay.cabalist.system.spell.Spell;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record HangingSpellsS2CPacket(int entityId, List<Entry> spells) implements CustomPacketPayload {

    public static final Type<HangingSpellsS2CPacket> TYPE = new Type<>(Cabalist.id("hanging_spells"));
    private static final Codec<HangingSpellsS2CPacket> DATA_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("entity").forGetter(HangingSpellsS2CPacket::entityId),
            Entry.CODEC.listOf().fieldOf("spells").forGetter(HangingSpellsS2CPacket::spells)
    ).apply(instance, HangingSpellsS2CPacket::new));
    public static final StreamCodec<ByteBuf, HangingSpellsS2CPacket> CODEC = ByteBufCodecs.fromCodec(DATA_CODEC);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(String words, boolean running, Palette palette, float charge, String awaiting, float flow, float remaining, Optional<UUID> arbiter) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("words").forGetter(Entry::words),
                Codec.BOOL.fieldOf("running").forGetter(Entry::running),
                Palette.CODEC.fieldOf("palette").forGetter(Entry::palette),
                Codec.FLOAT.optionalFieldOf("charge", 1f).forGetter(Entry::charge),
                Codec.STRING.optionalFieldOf("awaiting", "").forGetter(Entry::awaiting),
                Codec.FLOAT.optionalFieldOf("flow", 0f).forGetter(Entry::flow),
                Codec.FLOAT.optionalFieldOf("remaining", 1f).forGetter(Entry::remaining),
                UUIDUtil.CODEC.optionalFieldOf("arbiter").forGetter(Entry::arbiter)
        ).apply(instance, Entry::new));

        public Entry withArbiter(@Nullable UUID id) {
            return new Entry(words, running, palette, charge, awaiting, flow, remaining, Optional.ofNullable(id));
        }

        public static Entry of(PendingSpell pending, Palette palette, float charge) {
            float flow = 0;
            float remaining = 1;
            for (Spell spell : pending.getRunningSpells()) {
                flow += (float) spell.getLedger().getTotalRate();
                remaining = Math.min(remaining, spell.getRemainingFraction());
            }
            return new Entry(pending.getIncantation(), pending.isRunning(), palette, charge, pending.getAwaitingText(),
                    Math.round(flow * 2) / 2f, Math.round(remaining * 32) / 32f, Optional.empty());
        }
    }
}
