package xyz.volcanobay.cabalist.system.aspect;

import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.system.energy.EnergyStack;
import xyz.volcanobay.cabalist.system.energy.EnergyType;

/**
 * A positive amount gives energy to the spell's stack, a negative amount takes it.
 * Produced energy is added on top, but only counts if the spell spends it in the same tick.
 */
public class EnergyUse {
    private @Nullable EnergyType type;
    private double amount;
    private double produced;

    public void set(EnergyType type, double amount) {
        this.type = type;
        this.amount = amount;
    }

    public void setProduced(double produced) {
        this.produced = produced;
    }

    public double getProduced() {
        return produced;
    }

    public void clear() {
        type = null;
        amount = 0;
        produced = 0;
    }

    public @Nullable EnergyType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public void scale(double factor) {
        amount *= factor;
        produced *= factor;
    }

    public void applyTo(EnergyStack stack) {
        if (type == null) {
            return;
        }
        if (produced > 0) {
            stack.give(type, produced);
        }
        if (amount > 0) {
            stack.give(type, amount);
        } else {
            stack.take(type, -amount);
        }
    }
}
