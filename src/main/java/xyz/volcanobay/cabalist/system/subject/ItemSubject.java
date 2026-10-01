package xyz.volcanobay.cabalist.system.subject;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.core.CabalistDataComponents;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.blood.BloodStain;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.spell.SpellClause;

/**
 * Effects the item can't take fall back to its holder.
 */
public class ItemSubject extends Subject {
    private final ItemStack stack;
    private final Subject holder;

    public ItemSubject(ItemStack stack, Subject holder) {
        this.stack = stack;
        this.holder = holder;
    }

    public ItemStack getStack() {
        return stack;
    }

    public Subject getHolder() {
        return holder;
    }

    @Override
    public @Nullable Level getLevel() {
        return holder.getLevel();
    }

    @Override
    public void getPosition(Vector3d out) {
        holder.getPosition(out);
    }

    @Override
    public void getFacing(Vector3d out) {
        holder.getFacing(out);
    }

    @Override
    public void getCastOrigin(Vector3d out) {
        holder.getCastOrigin(out);
    }

    @Override
    public boolean receive(Aspect aspect, SpellClause clause, float magnitude) {
        if (aspect.affectItem(this, clause, magnitude)) {
            return true;
        }
        return holder.receive(aspect, clause, magnitude);
    }

    @Override
    public @Nullable Subject getEnemy() {
        return holder.getEnemy();
    }

    @Override
    public boolean represents(Entity entity) {
        return holder.represents(entity);
    }

    @Override
    public @Nullable BloodStain getBlood() {
        return stack.get(CabalistDataComponents.BLOOD.get());
    }

    @Override
    public @Nullable Contract getBoundContract() {
        return ContractSystem.INSTANCE.getContract(stack);
    }

    @Override
    public boolean isValid() {
        return !stack.isEmpty() && holder.isValid();
    }

    @Override
    public Component getDisplayName() {
        return stack.getHoverName();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ItemSubject item && item.stack == stack && item.holder.equals(holder);
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(stack) * 31 + holder.hashCode();
    }
}
