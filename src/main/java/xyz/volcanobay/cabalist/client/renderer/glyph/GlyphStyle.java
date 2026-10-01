package xyz.volcanobay.cabalist.client.renderer.glyph;

import net.minecraft.resources.ResourceLocation;

/**
 * Each style is a vanilla font sheet: 16 by 16 cells of 8 by 8 texels, indexed by their character codes.
 */
public enum GlyphStyle {
    RUNE("rune", ResourceLocation.withDefaultNamespace("textures/font/ascii_sga.png"), ResourceLocation.withDefaultNamespace("alt")),
    LATIN("latin", ResourceLocation.withDefaultNamespace("textures/font/ascii.png"), ResourceLocation.withDefaultNamespace("default"));

    private final String name;
    private final ResourceLocation sheet;
    private final ResourceLocation font;

    GlyphStyle(String name, ResourceLocation sheet, ResourceLocation font) {
        this.name = name;
        this.sheet = sheet;
        this.font = font;
    }

    public String getName() {
        return name;
    }

    public ResourceLocation getSheet() {
        return sheet;
    }

    public ResourceLocation getFont() {
        return font;
    }
}
