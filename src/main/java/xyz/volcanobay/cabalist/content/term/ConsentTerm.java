package xyz.volcanobay.cabalist.content.term;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.system.contract.Term;
import xyz.volcanobay.cabalist.system.spell.Spell;

public class ConsentTerm extends Term {
    private @Nullable String spellName;

    public ConsentTerm(ResourceLocation resourceLocation) {
        super(resourceLocation);
    }

    @Override
    public Term create() {
        return new ConsentTerm(getResourceLocation());
    }

    public @Nullable String getSpellName() {
        return spellName;
    }

    public void setSpellName(@Nullable String spellName) {
        this.spellName = spellName;
    }

    @Override
    public boolean grantsConsent(Spell spell) {
        return spellName != null && spellName.equals(spell.getName());
    }

    @Override
    public void read(CompoundTag tag) {
        spellName = tag.contains("spell") ? tag.getString("spell") : null;
        super.read(tag);
    }

    @Override
    public CompoundTag write(CompoundTag tag) {
        if (spellName != null) {
            tag.putString("spell", spellName);
        }
        return super.write(tag);
    }
}
