package xyz.volcanobay.cabalist.system.spell;

import net.minecraft.resources.ResourceLocation;
import xyz.volcanobay.cabalist.core.CabalistSpellDictionary;

/**
 * A part of a spell. Can be described as a "word," however, that term is inaccurate as parts can have multiple words.
 * Note that the resource location is used to identify the spell dictionary entry and to identify the part.
 */
public abstract class Part {
    private final ResourceLocation resourceLocation;
    private final SpellDictionary dictionary;

    public Part(ResourceLocation resourceLocation) {
        this.resourceLocation = resourceLocation;
        this.dictionary = CabalistSpellDictionary.getSpellDictionary(resourceLocation);
    }

    public SpellDictionary getDictionary() {
        return dictionary;
    }
}
