package xyz.volcanobay.cabalist.content.contract.terms;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import xyz.volcanobay.cabalist.system.contract.Term;

public class LimitTerm extends Term {
    private int limit;

    public LimitTerm(ResourceLocation resourceLocation) {
        super(resourceLocation);
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public void read(CompoundTag tag) {
        limit = tag.getInt("limit");
        super.read(tag);
    }

    @Override
    public CompoundTag write(CompoundTag tag) {
        tag.putInt("limit", limit);
        return super.write(tag);
    }
}
