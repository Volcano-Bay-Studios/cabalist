package xyz.volcanobay.cabalist.system.blood;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import xyz.volcanobay.cabalist.core.CabalistDataComponents;
import xyz.volcanobay.cabalist.core.CabalistGameEvents;
import xyz.volcanobay.cabalist.core.CabalistTerms;
import xyz.volcanobay.cabalist.system.spell.SpellTriggers;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.ItemSubject;

public class BloodHelper {
    public static void coverInBlood(ItemStack stack, LivingEntity holder, Entity bleeder, boolean consented) {
        if (stack.isEmpty() || holder.level().isClientSide) {
            return;
        }
        stack.set(CabalistDataComponents.BLOOD.get(), new BloodStain(bleeder.getUUID(), consented));
        holder.level().gameEvent(CabalistGameEvents.getHolder(CabalistGameEvents.BLOOD_SHED), bleeder.position(), GameEvent.Context.of(holder));
        SpellTriggers.raise(CabalistTerms.BLOODIED.getId(), new ItemSubject(stack, EntitySubject.of(holder)), EntitySubject.of(bleeder));
    }
}
