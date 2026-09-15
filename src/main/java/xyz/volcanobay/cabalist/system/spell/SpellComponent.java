package xyz.volcanobay.cabalist.system.spell;

import net.minecraft.resources.ResourceLocation;

/**
 * A part of a spell. Can be described as a "word," however, that term is inaccurate as parts can have multiple words.
 * Note that the resource location is used to identify the spell dictionary entry and to identify the part.
 */
public abstract class SpellComponent {
    private ResourceLocation resourceLocation = null;
    private SpellDictionary dictionary;

    public SpellComponent() {
    }

    public SpellDictionary getDictionary() {
        return dictionary;
    }

    public void setResourceLocation(ResourceLocation resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    public void setDictionary(SpellDictionary dictionary) {
        this.dictionary = dictionary;
    }
}
