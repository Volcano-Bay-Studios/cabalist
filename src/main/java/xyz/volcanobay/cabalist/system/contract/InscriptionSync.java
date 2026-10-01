package xyz.volcanobay.cabalist.system.contract;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import xyz.volcanobay.cabalist.core.CabalistDataComponents;
import xyz.volcanobay.cabalist.system.focus.Focus;
import xyz.volcanobay.cabalist.system.render.SpellVisuals;
import xyz.volcanobay.cabalist.system.spell.SpellEngine;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;

import java.util.ArrayList;
import java.util.List;

public final class InscriptionSync {
    public static void refresh(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            refresh(stack, player);
        }
        for (ItemStack stack : player.getInventory().armor) {
            refresh(stack, player);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            refresh(stack, player);
        }
    }

    private static void refresh(ItemStack stack, Player player) {
        Contract contract = ContractSystem.INSTANCE.getContract(stack);
        if (contract == null || !Focus.canHold(stack)) {
            stack.remove(CabalistDataComponents.INSCRIPTION.get());
            return;
        }
        List<String> texts = contract.getIncantations();
        Inscription current = stack.get(CabalistDataComponents.INSCRIPTION.get());
        if (current != null && current.hasTexts(texts)) {
            return;
        }
        List<Inscription.Line> lines = new ArrayList<>();
        for (String text : texts) {
            int tint = SpellVisuals.getPalette(SpellEngine.INSTANCE.resolve(text, EntitySubject.of(player), 0)).getPrimary();
            lines.add(new Inscription.Line(text, tint));
        }
        stack.set(CabalistDataComponents.INSCRIPTION.get(), new Inscription(lines));
    }
}
