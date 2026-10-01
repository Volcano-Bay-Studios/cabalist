package xyz.volcanobay.cabalist.system.render;

import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.form.Delivery;
import xyz.volcanobay.cabalist.system.form.DeliverySystem;
import xyz.volcanobay.cabalist.system.spell.HangingSpellSystem;
import xyz.volcanobay.cabalist.system.spell.PendingSpell;
import xyz.volcanobay.cabalist.system.spell.Spell;

import java.util.List;

public final class Recoloring {
    public static void recolor(Spell spell, List<Integer> colors) {
        for (Contract contract : ContractSystem.INSTANCE.getContracts()) {
            for (PendingSpell pending : contract.getPendingSpells()) {
                if (pending.getSpell() == spell || pending.getRunningSpells().contains(spell)) {
                    contract.recolor(pending.getIncantation(), colors);
                    return;
                }
            }
        }
        PendingSpell hanging = HangingSpellSystem.INSTANCE.find(spell);
        if (hanging != null) {
            apply(hanging, colors);
        } else {
            apply(spell, colors);
        }
    }

    public static void apply(PendingSpell pending, List<Integer> colors) {
        apply(pending.getSpell(), colors);
        for (Spell running : pending.getRunningSpells()) {
            apply(running, colors);
        }
    }

    public static void apply(Spell spell, List<Integer> colors) {
        spell.setColors(colors);
        Palette palette = new Palette(List.copyOf(colors));
        for (Delivery delivery : DeliverySystem.INSTANCE.getActive()) {
            if (delivery.getSpell() == spell) {
                delivery.getVisualIds().forEach(id -> SpellVisuals.recolor(delivery.getLevel(), id, palette));
            }
        }
    }
}
