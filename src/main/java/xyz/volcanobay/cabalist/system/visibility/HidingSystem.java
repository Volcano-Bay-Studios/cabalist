package xyz.volcanobay.cabalist.system.visibility;

import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.content.spell.aspect.HideAspect;
import xyz.volcanobay.cabalist.networking.packet.HiddenS2CPacket;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.form.Delivery;
import xyz.volcanobay.cabalist.system.form.DeliverySystem;
import xyz.volcanobay.cabalist.system.spell.HangingSpellSystem;
import xyz.volcanobay.cabalist.system.spell.PendingSpell;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.ItemSubject;
import xyz.volcanobay.cabalist.system.subject.RuneSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Hidden spells are revealed to things that are acted upon. Appraising a spell reveals it aswell.
 */
public class HidingSystem {
    public static final HidingSystem INSTANCE = new HidingSystem();
    public static final long RENEW_TICKS = 5;
    private static final long REVEAL_TICKS = 200;
    private static final int SYNC_INTERVAL_TICKS = 5;
    private static final float FAINT = 0.35f;
    private static final float ACTED_ON = 0.8f;
    private static final long ACTED_ON_HOLD_TICKS = 2;
    private static final float ACTED_ON_FADE_TICKS = 60;
    private static final float STEPS = 20;

    private final Map<UUID, Long> entities = new HashMap<>();
    private final Map<UUID, Long> contracts = new HashMap<>();
    private final Map<Spell, Long> spells = Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<UUID, Map<UUID, Long>> contractReveals = new HashMap<>();
    private final Map<UUID, HiddenS2CPacket> synced = new HashMap<>();
    private final Map<PendingSpell, Run> runs = new WeakHashMap<>();

    private static class Run {
        private int fires;
        private long at = Long.MIN_VALUE;
    }

    public void hideEntity(UUID entity, long gameTime) {
        entities.put(entity, gameTime + RENEW_TICKS);
    }

    public void hideContract(UUID contract, long gameTime) {
        contracts.put(contract, gameTime + RENEW_TICKS);
    }

    public void hideSpell(Spell spell, long gameTime) {
        spells.put(spell, gameTime + RENEW_TICKS);
    }

    public void revealContract(UUID contract, UUID viewer, long gameTime) {
        contractReveals.computeIfAbsent(contract, key -> new HashMap<>()).put(viewer, gameTime);
    }

    public @Nullable Long getContractReveal(UUID contract, UUID viewer) {
        Map<UUID, Long> reveals = contractReveals.get(contract);
        return reveals == null ? null : reveals.get(viewer);
    }

    public boolean isHidden(Spell spell) {
        return spells.containsKey(spell);
    }

    public void prune(long gameTime) {
        entities.values().removeIf(until -> until < gameTime);
        contracts.values().removeIf(until -> until < gameTime);
        contractReveals.values().forEach(reveals -> reveals.values().removeIf(at -> gameTime - at > REVEAL_TICKS));
        contractReveals.values().removeIf(Map::isEmpty);
        spells.entrySet().removeIf(entry -> entry.getValue() < gameTime || entry.getKey().isDismissed());
    }

    public void clear() {
        synced.clear();
        runs.clear();
        entities.clear();
        contracts.clear();
        spells.clear();
        contractReveals.clear();
    }

    public void tick(MinecraftServer server) {
        if (server.getTickCount() % SYNC_INTERVAL_TICKS != 0) {
            return;
        }
        prune(server.overworld().getGameTime());
        trackRuns(server.overworld().getGameTime());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            HiddenS2CPacket packet = collect(player);
            if (!packet.equals(synced.get(player.getUUID()))) {
                synced.put(player.getUUID(), packet);
                VeilPacketManager.player(player).sendPacket(packet);
            }
        }
        synced.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    private void trackRuns(long gameTime) {
        for (Contract contract : ContractSystem.INSTANCE.getContracts()) {
            for (PendingSpell pending : contract.getPendingSpells()) {
                int fires = 0;
                for (int i = 0; i < pending.getClauseCount(); i++) {
                    fires += pending.getFireCount(i);
                }
                Run run = runs.get(pending);
                if (run == null) {
                    run = new Run();
                    run.fires = fires;
                    runs.put(pending, run);
                }
                if (pending.isRunning() || fires != run.fires) {
                    run.at = gameTime;
                }
                run.fires = fires;
            }
        }
    }

    private HiddenS2CPacket collect(ServerPlayer viewer) {
        ServerLevel level = viewer.serverLevel();
        List<HiddenS2CPacket.Mark> entities = new ArrayList<>();
        for (UUID hidden : this.entities.keySet()) {
            Entity entity = level.getEntity(hidden);
            if (entity != null) {
                entities.add(new HiddenS2CPacket.Mark(entity.getId(), hidden.equals(viewer.getUUID()) ? FAINT : 0));
            }
        }
        List<HiddenS2CPacket.Mark> visuals = new ArrayList<>();
        for (Delivery delivery : DeliverySystem.INSTANCE.getActive()) {
            Spell spell = delivery.getSpell();
            if (spell != null && isHidden(spell) && delivery.getLevel() == level) {
                float alpha = getAlpha(viewer, spell);
                delivery.getVisualIds().forEach(id -> visuals.add(new HiddenS2CPacket.Mark(id, alpha)));
            }
        }
        List<HiddenS2CPacket.CircleMark> circles = new ArrayList<>();
        for (Subject host : HangingSpellSystem.INSTANCE.getHosts()) {
            if (host instanceof EntitySubject entityHost && entityHost.getEntity().level() == level) {
                for (PendingSpell pending : HangingSpellSystem.INSTANCE.get(host)) {
                    Spell spell = pending.getSpell();
                    if (isHidden(spell) || HideAspect.hidesItself(spell) && !pending.isRunning()) {
                        circles.add(new HiddenS2CPacket.CircleMark(entityHost.getEntity().getId(), pending.getIncantation().hashCode(), getAlpha(viewer, spell)));
                    }
                }
            }
        }
        List<HiddenS2CPacket.BlockMark> blocks = new ArrayList<>();
        List<HiddenS2CPacket.BlockCircleMark> blockCircles = new ArrayList<>();
        for (Contract contract : ContractSystem.INSTANCE.getContracts()) {
            Subject host = contract.getHost();
            boolean isHidden = contracts.containsKey(contract.getUUID());
            float contractAlpha = isHidden ? round(Math.max(contract.isContractee(viewer.getUUID()) ? FAINT : 0,
                    getShownAlpha(viewer, getContractReveal(contract.getUUID(), viewer.getUUID())))) : 1;
            if (host instanceof RuneSubject rune && rune.getLevel() == level) {
                if (isHidden) {
                    blocks.add(new HiddenS2CPacket.BlockMark(rune.getPos(), contractAlpha));
                }
                for (PendingSpell pending : contract.getPendingSpells()) {
                    if (HideAspect.hidesItself(pending.getSpell())) {
                        blockCircles.add(new HiddenS2CPacket.BlockCircleMark(rune.getPos(), pending.getIncantation().hashCode(), getInscribedAlpha(viewer, contract, pending)));
                    }
                }
            } else if (host instanceof ItemSubject item && item.getHolder() instanceof EntitySubject holder && holder.getEntity().level() == level) {
                for (PendingSpell pending : contract.getPendingSpells()) {
                    float alpha = HideAspect.hidesItself(pending.getSpell()) ? Math.min(contractAlpha, getInscribedAlpha(viewer, contract, pending)) : contractAlpha;
                    if (alpha < 1) {
                        circles.add(new HiddenS2CPacket.CircleMark(holder.getEntity().getId(), pending.getIncantation().hashCode(), alpha));
                    }
                }
            }
        }
        return new HiddenS2CPacket(entities, visuals, blocks, circles, blockCircles);
    }

    private float getInscribedAlpha(ServerPlayer viewer, Contract contract, PendingSpell pending) {
        if (pending.isRunning()) {
            return 1;
        }
        boolean isOwner = !pending.getSpell().hidesFromOwners() && contract.isContractee(viewer.getUUID());
        Run run = runs.get(pending);
        float alpha = Math.max(isOwner ? FAINT : 0, getShownAlpha(viewer, run == null || run.at == Long.MIN_VALUE ? null : run.at));
        alpha = Math.max(alpha, getShownAlpha(viewer, getContractReveal(contract.getUUID(), viewer.getUUID())));
        return round(alpha);
    }

    private static float round(float alpha) {
        return Math.round(alpha * STEPS) / STEPS;
    }

    private static float getAlpha(ServerPlayer viewer, Spell spell) {
        UUID id = viewer.getUUID();
        Subject host = spell.getHost();
        Contract contract = host == null ? null : host.getBoundContract();
        boolean isOwner = !spell.hidesFromOwners() && (host != null && id.equals(host.getUUID()) || contract != null && contract.isContractee(viewer.getUUID()));
        float alpha = isOwner ? FAINT : 0;
        alpha = Math.max(alpha, getShownAlpha(viewer, spell.getActedOn().get(id)));
        if (spell.getRevealedToAllAt() != Long.MIN_VALUE) {
            alpha = Math.max(alpha, getShownAlpha(viewer, spell.getRevealedToAllAt()));
        }
        return round(alpha);
    }

    private static float getShownAlpha(ServerPlayer viewer, @Nullable Long shownAt) {
        if (shownAt == null) {
            return 0;
        }
        long since = viewer.level().getGameTime() - shownAt - ACTED_ON_HOLD_TICKS;
        return ACTED_ON * Math.max(0, 1 - Math.max(0, since) / ACTED_ON_FADE_TICKS);
    }
}
