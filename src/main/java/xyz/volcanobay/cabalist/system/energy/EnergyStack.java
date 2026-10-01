package xyz.volcanobay.cabalist.system.energy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;

import java.util.Arrays;

/**
 * Energy held by a subject. Amounts never go below zero; a missing type falls back to entropy,
 * and entropy the stack can't cover is pulled from the world as pressure.
 */
public class EnergyStack implements INBTSerializable<CompoundTag> {
    private final double[] amounts = new double[EnergyType.getCount()];
    private double worldPressure;

    public double get(EnergyType type) {
        return amounts[type.getId()];
    }

    public double getWorldPressure() {
        return worldPressure;
    }

    public void clearWorldPressure() {
        worldPressure = 0;
    }

    protected void addWorldPressure(double pressure) {
        worldPressure += pressure;
    }

    public void give(EnergyType type, double amount) {
        if (amount <= 0) {
            return;
        }
        amounts[type.getId()] += amount;
    }

    public void take(EnergyType type, double amount) {
        if (amount <= 0) {
            return;
        }
        double fromSameType = Math.min(amounts[type.getId()], amount);
        amounts[type.getId()] -= fromSameType;
        double remaining = amount - fromSameType;
        if (remaining <= 0) {
            return;
        }
        worldPressure += remaining - extract(CabalistEnergyTypes.ENTROPY.get(), remaining);
    }

    public void copyFrom(EnergyStack other) {
        for (EnergyType type : CabalistEnergyTypes.ENERGY_TYPE_REGISTRY) {
            amounts[type.getId()] = other.get(type);
        }
        worldPressure = other.worldPressure;
    }

    public double extract(EnergyType type, double max) {
        double extracted = Math.min(amounts[type.getId()], Math.max(max, 0));
        amounts[type.getId()] -= extracted;
        return extracted;
    }

    /**
     * Moves energy between types within this stack. Free, and limited to what the stack holds.
     */
    public double convert(EnergyType from, EnergyType to, double amount) {
        double moved = Math.min(amounts[from.getId()], Math.max(amount, 0));
        amounts[from.getId()] -= moved;
        amounts[to.getId()] += moved;
        return moved;
    }

    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        return write(new CompoundTag());
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag tag) {
        read(tag);
    }

    public CompoundTag write(CompoundTag tag) {
        CompoundTag amountsTag = new CompoundTag();
        for (EnergyType type : CabalistEnergyTypes.ENERGY_TYPE_REGISTRY) {
            if (amounts[type.getId()] > 0) {
                amountsTag.putDouble(String.valueOf(CabalistEnergyTypes.ENERGY_TYPE_REGISTRY.getKey(type)), amounts[type.getId()]);
            }
        }
        tag.put("amounts", amountsTag);
        tag.putDouble("world_pressure", worldPressure);
        return tag;
    }

    public void read(CompoundTag tag) {
        CompoundTag amountsTag = tag.getCompound("amounts");
        Arrays.fill(amounts, 0);
        for (String key : amountsTag.getAllKeys()) {
            ResourceLocation location = ResourceLocation.tryParse(key);
            EnergyType type = location == null ? null : CabalistEnergyTypes.ENERGY_TYPE_REGISTRY.get(location);
            if (type != null) {
                amounts[type.getId()] = amountsTag.getDouble(key);
            }
        }
        worldPressure = tag.getDouble("world_pressure");
    }
}
