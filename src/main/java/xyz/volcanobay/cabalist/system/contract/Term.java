package xyz.volcanobay.cabalist.system.contract;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CountDownLatch;

/**
 * Contracts hold terms.
 * They have effects on many things, and they only apply to the contract and its contractees.
 * You can apply terms in many ways, some examples include: checking if a contractee has a term, using a
 */
public class Term {
    private final ResourceLocation resourceLocation;

    public Term(ResourceLocation resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return resourceLocation.hashCode();
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
