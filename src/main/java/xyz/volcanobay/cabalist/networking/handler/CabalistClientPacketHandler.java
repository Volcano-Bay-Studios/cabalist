package xyz.volcanobay.cabalist.networking.handler;

import foundry.veil.api.network.handler.ClientPacketContext;
import xyz.volcanobay.cabalist.client.casting.ClientCastingSystem;
import xyz.volcanobay.cabalist.client.request.ClientRequestSystem;
import xyz.volcanobay.cabalist.client.request.DraftCircles;
import xyz.volcanobay.cabalist.client.request.RequestReading;
import xyz.volcanobay.cabalist.client.spell.ClientHangingSpellSystem;
import xyz.volcanobay.cabalist.client.spell.ClientSpellVisualSystem;
import xyz.volcanobay.cabalist.client.spell.SpellShake;
import xyz.volcanobay.cabalist.client.visibility.ClientHidingSystem;
import xyz.volcanobay.cabalist.networking.packet.BarriersS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.CastingEndS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.CastingStateS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.DraftsS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.HangingSpellsS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.HiddenS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.InscribedSpellsS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.RequestVerdictS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.RequestsS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.SpellEnergyS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.SpellShakeS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.SpellVisualEndS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.SpellVisualPaletteS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.SpellVisualS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.SpellVisualUpdateS2CPacket;
import xyz.volcanobay.cabalist.system.barrier.BarrierSystem;

public class CabalistClientPacketHandler {

    public static void handleSpellVisual(SpellVisualS2CPacket packet, ClientPacketContext ctx) {
        ClientSpellVisualSystem.INSTANCE.add(packet);
    }

    public static void handleSpellVisualPalette(SpellVisualPaletteS2CPacket packet, ClientPacketContext ctx) {
        ClientSpellVisualSystem.INSTANCE.setPalette(packet.id(), packet.palette());
    }

    public static void handleSpellVisualUpdate(SpellVisualUpdateS2CPacket packet, ClientPacketContext ctx) {
        ClientSpellVisualSystem.INSTANCE.update(packet.id(), packet.shape());
    }

    public static void handleCastingState(CastingStateS2CPacket packet, ClientPacketContext ctx) {
        ClientCastingSystem.INSTANCE.onState(packet);
    }

    public static void handleCastingEnd(CastingEndS2CPacket packet, ClientPacketContext ctx) {
        ClientCastingSystem.INSTANCE.onEnd(packet);
    }

    public static void handleRequestVerdict(RequestVerdictS2CPacket packet, ClientPacketContext ctx) {
        RequestReading.INSTANCE.onVerdict(packet);
    }

    public static void handleBarriers(BarriersS2CPacket packet, ClientPacketContext ctx) {
        BarrierSystem.CLIENT.set(packet.dimension(), packet.barriers());
    }

    public static void handleHidden(HiddenS2CPacket packet, ClientPacketContext ctx) {
        ClientHidingSystem.INSTANCE.set(packet);
    }

    public static void handleDrafts(DraftsS2CPacket packet, ClientPacketContext ctx) {
        DraftCircles.INSTANCE.set(packet.drafts());
    }

    public static void handleRequests(RequestsS2CPacket packet, ClientPacketContext ctx) {
        ClientRequestSystem.INSTANCE.set(packet.entityId(), packet.requests());
    }

    public static void handleHangingSpells(HangingSpellsS2CPacket packet, ClientPacketContext ctx) {
        ClientHangingSpellSystem.INSTANCE.set(packet.entityId(), packet.spells());
    }

    public static void handleInscribedSpells(InscribedSpellsS2CPacket packet, ClientPacketContext ctx) {
        if (packet.block().isPresent()) {
            ClientHangingSpellSystem.INSTANCE.setInscribedOnBlock(packet.block().get(), packet.spells());
        } else {
            ClientHangingSpellSystem.INSTANCE.setInscribedOnEntity(packet.entityId(), packet.spells());
        }
    }

    public static void handleSpellEnergy(SpellEnergyS2CPacket packet, ClientPacketContext ctx) {
        for (SpellEnergyS2CPacket.Entry entry : packet.entries()) {
            ClientSpellVisualSystem.INSTANCE.setEnergy(entry.visualId(), entry.flow(), entry.remaining());
        }
    }

    public static void handleSpellShake(SpellShakeS2CPacket packet, ClientPacketContext ctx) {
        SpellShake.add(packet.at(), packet.energy());
    }

    public static void handleSpellVisualEnd(SpellVisualEndS2CPacket packet, ClientPacketContext ctx) {
        ClientSpellVisualSystem.INSTANCE.end(packet.id());
    }
}
