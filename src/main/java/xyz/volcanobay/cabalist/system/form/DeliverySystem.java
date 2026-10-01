package xyz.volcanobay.cabalist.system.form;

import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.networking.packet.SpellEnergyS2CPacket;
import xyz.volcanobay.cabalist.system.spell.Spell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeliverySystem {
    public static final DeliverySystem INSTANCE = new DeliverySystem();

    private final List<Delivery> active = new ArrayList<>();
    private final List<Delivery> ticking = new ArrayList<>();
    private static final int ENERGY_SYNC_TICKS = 5;
    private int ticksSinceEnergySync;

    public void add(Delivery delivery) {
        active.add(delivery);
    }

    public void tick() {
        ticking.clear();
        ticking.addAll(active);
        for (Delivery delivery : ticking) {
            if (delivery.tick()) {
                active.remove(delivery);
                delivery.end();
            }
        }
        settleProduced();
        if (++ticksSinceEnergySync >= ENERGY_SYNC_TICKS) {
            ticksSinceEnergySync = 0;
            syncEnergy();
        }
    }

    private void syncEnergy() {
        Map<ServerLevel, List<SpellEnergyS2CPacket.Entry>> byLevel = new HashMap<>();
        for (Delivery delivery : active) {
            if (delivery.getLevel() instanceof ServerLevel level) {
                delivery.collectEnergy(byLevel.computeIfAbsent(level, key -> new ArrayList<>()));
            }
        }
        for (Map.Entry<ServerLevel, List<SpellEnergyS2CPacket.Entry>> entry : byLevel.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                VeilPacketManager.level(entry.getKey()).sendPacket(new SpellEnergyS2CPacket(entry.getValue()));
            }
        }
    }

    public List<Delivery> getActive() {
        return active;
    }

    private void settleProduced() {
        Set<Spell> settled = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Delivery delivery : ticking) {
            Spell spell = delivery.getSpell();
            if (spell != null && settled.add(spell)) {
                spell.settleProduced();
            }
        }
    }

    public @Nullable Spell findSpell(int visualId) {
        for (Delivery delivery : active) {
            if (delivery.hasVisual(visualId)) {
                return delivery.getSpell();
            }
        }
        return null;
    }

    public void clear() {
        active.clear();
    }
}
