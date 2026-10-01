package xyz.volcanobay.cabalist.networking.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.system.render.Palette;

public record SpellVisualPaletteS2CPacket(int id, Palette palette) implements CustomPacketPayload {
    public static final Type<SpellVisualPaletteS2CPacket> TYPE = new Type<>(Cabalist.id("spell_visual_palette"));
    public static final StreamCodec<ByteBuf, SpellVisualPaletteS2CPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpellVisualPaletteS2CPacket::id,
            Palette.STREAM_CODEC, SpellVisualPaletteS2CPacket::palette,
            SpellVisualPaletteS2CPacket::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
