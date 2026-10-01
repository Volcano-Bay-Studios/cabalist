package xyz.volcanobay.cabalist.system.focus;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.core.CabalistDataComponents;
import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.cabalist.item.FocusItem;
import xyz.volcanobay.cabalist.system.spell.Domain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.UnaryOperator;

/**
 * The held focus plus any imbued armor, which is treated as part of the cast.
 */
public class Focus {
    private static final double ATTUNEMENT_RANGE = 2;
    private static final double ATTUNEMENT_SCALE = 5000;
    private static final double WEAK_LIFEFORCE = 10;
    private static final double WEAK_CHARGE_SECONDS = 5;
    private static final double FASTEST_CHARGE_SPEEDUP = 1.7;

    private final List<ItemStack> stacks;
    private final @Nullable Imbuement fixed;

    private Focus(List<ItemStack> stacks, @Nullable Imbuement fixed) {
        this.stacks = stacks;
        this.fixed = fixed;
    }

    public static Focus standard() {
        return new Focus(List.of(), FocusItem.STANDARD.get());
    }

    public static @Nullable Focus of(LivingEntity caster) {
        ItemStack held = caster.getMainHandItem();
        if (!canHold(held) || get(held).getTotalLifeforce() <= 0) {
            return null;
        }
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(held);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack worn = caster.getItemBySlot(slot);
            if (slot.isArmor() && canHold(worn) && !get(worn).isEmpty()) {
                stacks.add(worn);
            }
        }
        return new Focus(stacks, null);
    }

    public static @Nullable Focus carriedBy(Player player) {
        Focus held = of(player);
        if (held != null) {
            return held;
        }
        ItemStack best = ItemStack.EMPTY;
        double most = 0;
        for (ItemStack stack : player.getInventory().items) {
            double lifeforce = canHold(stack) ? get(stack).getTotalLifeforce() : 0;
            if (lifeforce > most) {
                best = stack;
                most = lifeforce;
            }
        }
        return best.isEmpty() ? null : new Focus(List.of(best), null);
    }

    public static List<ItemStack> getActive(LivingEntity entity) {
        List<ItemStack> active = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);
            boolean worn = slot.isArmor() || slot.getType() == EquipmentSlot.Type.HAND;
            if (worn && canHold(stack) && (get(stack).getTotalLifeforce() > 0 || get(stack).receptive())) {
                active.add(stack);
            }
        }
        return active;
    }

    public static boolean canHold(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxStackSize() == 1;
    }

    public static Imbuement get(ItemStack stack) {
        Imbuement own = getStored(stack);
        return stack.getItem() instanceof FocusItem focusItem ? focusItem.getImbued().plus(own) : own;
    }

    public static Imbuement getStored(ItemStack stack) {
        return stack.getOrDefault(CabalistDataComponents.IMBUEMENT.get(), Imbuement.EMPTY);
    }

    public static double getRoom(ItemStack stack, ResourceLocation type) {
        return Math.max(0, LifeforceCaps.get(type) - get(stack).getLifeforce(type));
    }

    public static double give(ItemStack stack, ResourceLocation type, double amount) {
        if (!canHold(stack)) {
            return 0;
        }
        double taken = Math.min(amount, getRoom(stack, type));
        if (taken > 0) {
            update(stack, imbuement -> imbuement.withLifeforce(type, taken));
        }
        return Math.max(0, taken);
    }

    public static void update(ItemStack stack, UnaryOperator<Imbuement> change) {
        Imbuement changed = change.apply(stack.getOrDefault(CabalistDataComponents.IMBUEMENT.get(), Imbuement.EMPTY));
        if (changed.isEmpty()) {
            stack.remove(CabalistDataComponents.IMBUEMENT.get());
        } else {
            if (changed.seed() == 0) {
                changed = changed.withSeed(ThreadLocalRandom.current().nextInt() | 1);
            }
            stack.set(CabalistDataComponents.IMBUEMENT.get(), changed);
        }
    }

    public Imbuement getCombined() {
        Imbuement combined = fixed == null ? Imbuement.EMPTY : fixed;
        for (ItemStack stack : stacks) {
            combined = combined.plus(get(stack));
        }
        return combined;
    }

    public double getSafeMaximum() {
        return getCombined().getTotalLifeforce();
    }

    public double getChargeSeconds() {
        double standard = CabalistConfig.STANDARD_CHARGE_SECONDS.get();
        double standardLifeforce = FocusItem.STANDARD.get().getTotalLifeforce();
        double lifeforce = getSafeMaximum();
        double paceAtStandard = 1 / FASTEST_CHARGE_SPEEDUP;
        double paceAtWeak = standard / (WEAK_CHARGE_SECONDS * FASTEST_CHARGE_SPEEDUP);
        if (lifeforce <= 0 || standard <= 0 || paceAtWeak >= paceAtStandard || standardLifeforce <= WEAK_LIFEFORCE) {
            return standard;
        }
        double oddsAtStandard = paceAtStandard / (1 - paceAtStandard);
        double oddsAtWeak = paceAtWeak / (1 - paceAtWeak);
        double exponent = Math.log(oddsAtStandard / oddsAtWeak) / Math.log(standardLifeforce / WEAK_LIFEFORCE);
        double odds = oddsAtStandard * Math.pow(lifeforce / standardLifeforce, exponent);
        return standard * (1 + odds) / (FASTEST_CHARGE_SPEEDUP * odds);
    }

    public double getThroughput(@Nullable Domain domain) {
        double base = CabalistConfig.GATHER_RATE_PER_SECOND.get() * (1 + CabalistConfig.LIFEFORCE_GATHER_SPEEDUP.get() * (1 - Math.exp(-getSafeMaximum() / CabalistConfig.LIFEFORCE_GATHER_SCALE.get())));
        if (domain == null || stacks.isEmpty()) {
            return base;
        }
        double attunement = get(stacks.get(0)).getAttunement(getId(domain));
        return base * (1 + ATTUNEMENT_RANGE * (1 - Math.exp(-attunement / ATTUNEMENT_SCALE)));
    }

    public void attune(@Nullable Domain domain, double energy) {
        if (domain != null && !stacks.isEmpty() && energy > 0) {
            update(stacks.get(0), imbuement -> imbuement.withAttunement(getId(domain), energy));
        }
    }

    private static ResourceLocation getId(Domain domain) {
        ResourceLocation id = CabalistSpellComponents.PART_REGISTRY.getKey(domain);
        return id == null ? ResourceLocation.withDefaultNamespace("empty") : id;
    }
}
