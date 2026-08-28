package xyz.volcanobay.cabalist.system.contract;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class TermSet extends HashMap<ResourceLocation, Set<Term>> {
    public void add(Term term) {
        Set<Term> terms = this.computeIfAbsent(term.getResourceLocation(), (resourceLocation -> new LinkedHashSet<>()));
        terms.add(term);
    }

    public @NotNull Set<Term> get(ResourceLocation resourceLocation) {
        return this.getOrDefault(resourceLocation, new LinkedHashSet<>());
    }
}
