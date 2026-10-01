package xyz.volcanobay.cabalist.networking;

import foundry.veil.api.network.VeilPacketManager;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.networking.handler.CabalistClientPacketHandler;
import xyz.volcanobay.cabalist.networking.handler.CabalistServerPacketHandler;
import xyz.volcanobay.cabalist.networking.packet.CastingDismissC2SPacket;
import xyz.volcanobay.cabalist.networking.packet.CastingEndS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.CastingInputC2SPacket;
import xyz.volcanobay.cabalist.networking.packet.CastingStateS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.DraftsS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.HangingSpellsS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.BarriersS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.HiddenS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.InscribedSpellsS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.LookTargetC2SPacket;
import xyz.volcanobay.cabalist.networking.packet.RequestActionC2SPacket;
import xyz.volcanobay.cabalist.networking.packet.RequestAnswerC2SPacket;
import xyz.volcanobay.cabalist.networking.packet.RequestVerdictS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.RequestsS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.SpellEnergyS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.SpellShakeS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.SpellVisualEndS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.SpellVisualS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.SpellVisualPaletteS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.SpellVisualUpdateS2CPacket;

public class CabalistMessages {

    private static final VeilPacketManager INSTANCE = VeilPacketManager.create(Cabalist.MODID, "1");

    public static void register() {
        INSTANCE.registerClientbound(SpellVisualS2CPacket.TYPE, SpellVisualS2CPacket.CODEC, CabalistClientPacketHandler::handleSpellVisual);
        INSTANCE.registerClientbound(SpellVisualUpdateS2CPacket.TYPE, SpellVisualUpdateS2CPacket.CODEC, CabalistClientPacketHandler::handleSpellVisualUpdate);
        INSTANCE.registerClientbound(SpellVisualPaletteS2CPacket.TYPE, SpellVisualPaletteS2CPacket.CODEC, CabalistClientPacketHandler::handleSpellVisualPalette);
        INSTANCE.registerClientbound(HangingSpellsS2CPacket.TYPE, HangingSpellsS2CPacket.CODEC, CabalistClientPacketHandler::handleHangingSpells);
        INSTANCE.registerClientbound(HiddenS2CPacket.TYPE, HiddenS2CPacket.CODEC, CabalistClientPacketHandler::handleHidden);
        INSTANCE.registerClientbound(BarriersS2CPacket.TYPE, BarriersS2CPacket.CODEC, CabalistClientPacketHandler::handleBarriers);
        INSTANCE.registerClientbound(DraftsS2CPacket.TYPE, DraftsS2CPacket.CODEC, CabalistClientPacketHandler::handleDrafts);
        INSTANCE.registerClientbound(RequestsS2CPacket.TYPE, RequestsS2CPacket.CODEC, CabalistClientPacketHandler::handleRequests);
        INSTANCE.registerClientbound(InscribedSpellsS2CPacket.TYPE, InscribedSpellsS2CPacket.CODEC, CabalistClientPacketHandler::handleInscribedSpells);
        INSTANCE.registerClientbound(SpellEnergyS2CPacket.TYPE, SpellEnergyS2CPacket.CODEC, CabalistClientPacketHandler::handleSpellEnergy);
        INSTANCE.registerClientbound(SpellShakeS2CPacket.TYPE, SpellShakeS2CPacket.CODEC, CabalistClientPacketHandler::handleSpellShake);
        INSTANCE.registerClientbound(CastingStateS2CPacket.TYPE, CastingStateS2CPacket.CODEC, CabalistClientPacketHandler::handleCastingState);
        INSTANCE.registerClientbound(CastingEndS2CPacket.TYPE, CastingEndS2CPacket.CODEC, CabalistClientPacketHandler::handleCastingEnd);
        INSTANCE.registerServerbound(CastingInputC2SPacket.TYPE, CastingInputC2SPacket.CODEC, CabalistServerPacketHandler::handleCastingInput);
        INSTANCE.registerServerbound(LookTargetC2SPacket.TYPE, LookTargetC2SPacket.CODEC, CabalistServerPacketHandler::handleLookTarget);
        INSTANCE.registerServerbound(RequestAnswerC2SPacket.TYPE, RequestAnswerC2SPacket.CODEC, CabalistServerPacketHandler::handleRequestAnswer);
        INSTANCE.registerClientbound(RequestVerdictS2CPacket.TYPE, RequestVerdictS2CPacket.CODEC, CabalistClientPacketHandler::handleRequestVerdict);
        INSTANCE.registerServerbound(RequestActionC2SPacket.TYPE, RequestActionC2SPacket.CODEC, CabalistServerPacketHandler::handleRequestAction);
        INSTANCE.registerServerbound(CastingDismissC2SPacket.TYPE, CastingDismissC2SPacket.CODEC, CabalistServerPacketHandler::handleCastingDismiss);
        INSTANCE.registerClientbound(SpellVisualEndS2CPacket.TYPE, SpellVisualEndS2CPacket.CODEC, CabalistClientPacketHandler::handleSpellVisualEnd);
    }
}
