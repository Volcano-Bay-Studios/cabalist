package xyz.volcanobay.cabalist.system.contract;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.core.CabalistTerms;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class TermSet extends HashMap<ResourceLocation, Set<Term>> {
    public void add(Term term) {
        Set<Term> terms = this.computeIfAbsent(term.getResourceLocation(), (resourceLocation -> new LinkedHashSet<>()));
        terms.add(term);
    }

    public void addAll(TermSet other) {
        for (Set<Term> terms : other.values()) {
            for (Term term : terms) {
                add(term);
            }
        }
    }

    /**
     * Returns an immutable empty set if there are no terms of this type.
     */
    public @NotNull Set<Term> get(ResourceLocation resourceLocation) {
        return this.getOrDefault(resourceLocation, Set.of());
    }

    public ListTag write() {
        ListTag list = new ListTag();
        for (Set<Term> terms : values()) {
            for (Term term : terms) {
                CompoundTag tag = term.write(new CompoundTag());
                tag.putString("id", term.getResourceLocation().toString());
                list.add(tag);
            }
        }
        return list;
    }

    public void read(ListTag list) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
            Term template = id == null ? null : CabalistTerms.TERM_REGISTRY.get(id);
            if (template == null) {
                Cabalist.LOGGER.error("Unknown contract term {}", tag.getString("id"));
                continue;
            }
            Term term = template.create();
            term.read(tag);
            add(term);
        }
    }
}
