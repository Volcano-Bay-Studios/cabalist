package xyz.volcanobay.cabalist.system.focus;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import xyz.volcanobay.cabalist.core.CabalistDataComponents;
import xyz.volcanobay.cabalist.entity.LifeforceGlyph;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LifeforceFlow {
    private static final double BALANCE_RATE = 0.1;
    private static final double BALANCED = 0.01;

    public static boolean attracts(ItemStack stack, ResourceLocation type, boolean grounded) {
        if (!Focus.canHold(stack) || Focus.getRoom(stack, type) <= 0) {
            return false;
        }
        Imbuement imbuement = Focus.get(stack);
        return imbuement.getTotalLifeforce() > 0 || imbuement.receptive() || grounded && stack.has(CabalistDataComponents.BLOOD.get());
    }

    public static boolean attracts(LivingEntity entity, ResourceLocation type) {
        for (ItemStack stack : Focus.getActive(entity)) {
            if (Focus.getRoom(stack, type) > 0) {
                return true;
            }
        }
        return false;
    }

    public static double absorb(LivingEntity entity, ResourceLocation type, double amount) {
        List<ItemStack> active = Focus.getActive(entity);
        active.sort(Comparator.comparingDouble(stack -> Focus.get(stack).getLifeforce(type)));
        double taken = 0;
        for (ItemStack stack : active) {
            taken += Focus.give(stack, type, amount - taken);
        }
        return taken;
    }

    public static void balance(LivingEntity entity) {
        List<ItemStack> active = Focus.getActive(entity);
        if (active.size() < 2) {
            return;
        }
        Set<ResourceLocation> types = new HashSet<>();
        for (ItemStack stack : active) {
            types.addAll(Focus.get(stack).lifeforce().keySet());
        }
        for (ResourceLocation type : types) {
            balance(active, type);
        }
    }

    private static void balance(List<ItemStack> active, ResourceLocation type) {
        double[] amounts = new double[active.size()];
        double total = 0;
        double least = Double.MAX_VALUE;
        double most = 0;
        for (int i = 0; i < amounts.length; i++) {
            amounts[i] = Focus.get(active.get(i)).getLifeforce(type);
            total += amounts[i];
            least = Math.min(least, amounts[i]);
            most = Math.max(most, amounts[i]);
        }
        if (most - least < BALANCED) {
            return;
        }
        double even = total / amounts.length;
        double[] moved = new double[amounts.length];
        double pooled = 0;
        double missing = 0;
        for (int i = 0; i < amounts.length; i++) {
            ItemStack stack = active.get(i);
            if (amounts[i] <= even) {
                missing += even - amounts[i];
                continue;
            }
            double taken = Math.min(Focus.getStored(stack).getLifeforce(type), (amounts[i] - even) * BALANCE_RATE);
            Focus.update(stack, imbuement -> imbuement.withLifeforce(type, -taken));
            moved[i] = taken;
            pooled += taken;
        }
        if (pooled <= 0) {
            return;
        }
        double given = 0;
        for (int i = 0; i < amounts.length && missing > 0; i++) {
            if (amounts[i] < even) {
                given += Focus.give(active.get(i), type, pooled * (even - amounts[i]) / missing);
            }
        }
        double left = pooled - given;
        for (int i = 0; i < amounts.length && left > 0; i++) {
            double share = left * moved[i] / pooled;
            if (share > 0) {
                Focus.update(active.get(i), imbuement -> imbuement.withLifeforce(type, share));
            }
        }
    }

    public static boolean release(ItemStack stack, ServerLevel level, Vec3 at) {
        if (!Focus.canHold(stack)) {
            return false;
        }
        Imbuement stored = Focus.getStored(stack);
        Map<ResourceLocation, Double> lifeforce = stored.lifeforce();
        if (lifeforce.isEmpty() && !stored.receptive() && !stack.has(CabalistDataComponents.BLOOD.get())) {
            return false;
        }
        stack.remove(CabalistDataComponents.BLOOD.get());
        Focus.update(stack, Imbuement::withoutLifeforce);
        lifeforce.forEach((type, amount) -> LifeforceGlyph.scatter(level, at, type, amount, true));
        return true;
    }
}
