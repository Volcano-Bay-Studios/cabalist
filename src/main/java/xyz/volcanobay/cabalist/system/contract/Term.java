package xyz.volcanobay.cabalist.system.contract;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.TriggerEvent;
import xyz.volcanobay.cabalist.system.subject.Subject;

/**
 * Terms are like requirements.
 * They have effects on many things, and they only apply to the contract and its contractees.
 */
public class Term {
    private final ResourceLocation resourceLocation;

    public Term(ResourceLocation resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    @Override
    public int hashCode() {
        return resourceLocation.hashCode();
    }

    /**
     * The registered term is a template; this makes a new instance to hold data. Subclasses must override.
     */
    public Term create() {
        return new Term(resourceLocation);
    }

    public boolean isMetBy(TriggerEvent event, Subject host) {
        return true;
    }

    public boolean grantsConsent(Spell spell) {
        return false;
    }

    public CompoundTag write(CompoundTag tag) {
        return tag;
    }

    public void read(CompoundTag tag) {
    }

    public ResourceLocation getResourceLocation() {
        return resourceLocation;
    }
}
