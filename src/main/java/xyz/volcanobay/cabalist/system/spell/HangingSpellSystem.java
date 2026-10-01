package xyz.volcanobay.cabalist.system.spell;

import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.networking.packet.HangingSpellsS2CPacket;
import xyz.volcanobay.cabalist.system.render.SpellVisuals;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Spells upon their host, both those waiting on requirements and those running. Pay's upkeep from it's own
 * gathered energy.
 * Server thread only, and not saved.
 */
public class HangingSpellSystem {
    public static final HangingSpellSystem INSTANCE = new HangingSpellSystem();
    private static final int UPKEEP_INTERVAL_TICKS = 20;
    private static final int SYNC_INTERVAL_TICKS = 5;
    private static final int RESYNC_INTERVAL_TICKS = 100;

    private final Map<Subject, List<PendingSpell>> byHost = new HashMap<>();
    private final List<PendingSpell> triggering = new ArrayList<>();
    private final List<PendingSpell> waiting = new ArrayList<>();
    private final Map<Subject, List<HangingSpellsS2CPacket.Entry>> synced = new HashMap<>();

    public void add(PendingSpell pending) {
        byHost.computeIfAbsent(pending.getHost(), host -> new ArrayList<>()).add(pending);
        pending.getHost().onHangingSpellAdded();
    }

    public List<PendingSpell> get(Subject host) {
        List<PendingSpell> pending = byHost.get(host);
        return pending == null ? List.of() : pending;
    }

    public Set<Subject> getHosts() {
        return byHost.keySet();
    }

    public @Nullable PendingSpell find(Subject host, int index, int wordsHash) {
        List<PendingSpell> spells = get(host);
        if (index >= 0 && index < spells.size() && spells.get(index).getIncantation().hashCode() == wordsHash) {
            return spells.get(index);
        }
        for (PendingSpell pending : spells) {
            if (pending.getIncantation().hashCode() == wordsHash) {
                return pending;
            }
        }
        return null;
    }

    public boolean removeSpell(Spell spell) {
        for (List<PendingSpell> pendingSpells : byHost.values()) {
            for (Iterator<PendingSpell> iterator = pendingSpells.iterator(); iterator.hasNext(); ) {
                PendingSpell pending = iterator.next();
                if (pending.getSpell() == spell) {
                    pending.dismiss();
                    iterator.remove();
                    return true;
                }
            }
        }
        return false;
    }

    public void triggerAll(TriggerEvent event, long gameTime) {
        List<PendingSpell> all = new ArrayList<>();
        byHost.values().forEach(all::addAll);
        for (PendingSpell pending : all) {
            pending.onTrigger(event, gameTime);
        }
        byHost.values().forEach(pendingSpells -> pendingSpells.removeIf(PendingSpell::isComplete));
    }

    public void trigger(TriggerEvent event, long gameTime) {
        List<PendingSpell> pendingSpells = byHost.get(event.host());
        if (pendingSpells == null) {
            return;
        }
        triggering.clear();
        triggering.addAll(pendingSpells);
        for (PendingSpell pending : triggering) {
            pending.onTrigger(event, gameTime);
        }
        pendingSpells.removeIf(PendingSpell::isComplete);
    }

    public void tick(MinecraftServer server) {
        if (server.getTickCount() % SYNC_INTERVAL_TICKS == 0) {
            sync(server.getTickCount() % RESYNC_INTERVAL_TICKS == 0);
        }
        if (server.getTickCount() % UPKEEP_INTERVAL_TICKS != 0) {
            return;
        }
        for (Iterator<Map.Entry<Subject, List<PendingSpell>>> iterator = byHost.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Subject, List<PendingSpell>> entry = iterator.next();
            List<PendingSpell> pendingSpells = entry.getValue();
            if (!entry.getKey().isValid()) {
                pendingSpells.forEach(PendingSpell::release);
                iterator.remove();
                continue;
            }
            pendingSpells.removeIf(PendingSpell::isComplete);
            if (pendingSpells.isEmpty()) {
                iterator.remove();
                continue;
            }
            waiting.clear();
            for (PendingSpell pending : pendingSpells) {
                if (pending.isWaiting()) {
                    waiting.add(pending);
                }
            }
            if (waiting.isEmpty()) {
                continue;
            }
            for (PendingSpell pending : waiting) {
                if (!payUpkeep(pending)) {
                    pending.dismiss();
                    pendingSpells.remove(pending);
                }
            }
        }
    }

    private static boolean payUpkeep(PendingSpell pending) {
        double upkeep = CabalistConfig.HANGING_UPKEEP_PER_SECOND.get() * UPKEEP_INTERVAL_TICKS / 20.0;
        Spell spell = pending.getSpell();
        double paid = spell.getEnergyStack().extract(CabalistEnergyTypes.ENTROPY.get(), upkeep);
        spell.getLedger().spend(EnergyLedger.UPKEEP, paid);
        return paid >= upkeep;
    }

    private void sync(boolean resendAll) {
        for (Map.Entry<Subject, List<PendingSpell>> entry : byHost.entrySet()) {
            if (!(entry.getKey() instanceof EntitySubject host) || !host.isValid()) {
                continue;
            }
            List<HangingSpellsS2CPacket.Entry> spells = new ArrayList<>();
            for (PendingSpell pending : entry.getValue()) {
                spells.add(HangingSpellsS2CPacket.Entry.of(pending, SpellVisuals.getPalette(pending.getSpell()), 1));
            }
            if (resendAll || !spells.equals(synced.get(host))) {
                synced.put(host, spells);
                VeilPacketManager.trackingAndSelf(host.getEntity()).sendPacket(new HangingSpellsS2CPacket(host.getEntity().getId(), spells));
            }
        }
        for (Iterator<Map.Entry<Subject, List<HangingSpellsS2CPacket.Entry>>> iterator = synced.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Subject, List<HangingSpellsS2CPacket.Entry>> entry = iterator.next();
            List<PendingSpell> pendingSpells = byHost.get(entry.getKey());
            if (pendingSpells == null || pendingSpells.isEmpty()) {
                iterator.remove();
                if (entry.getKey() instanceof EntitySubject host && !host.getEntity().isRemoved()) {
                    VeilPacketManager.trackingAndSelf(host.getEntity()).sendPacket(new HangingSpellsS2CPacket(host.getEntity().getId(), List.of()));
                }
            }
        }
    }

    public @Nullable PendingSpell find(Spell spell) {
        for (List<PendingSpell> pendingSpells : byHost.values()) {
            for (PendingSpell pending : pendingSpells) {
                if (pending.getSpell() == spell) {
                    return pending;
                }
            }
        }
        return null;
    }

    public void clear() {
        byHost.clear();
        synced.clear();
        waiting.clear();
    }
}
