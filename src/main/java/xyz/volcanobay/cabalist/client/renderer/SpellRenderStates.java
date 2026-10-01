package xyz.volcanobay.cabalist.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderBridge;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;
import xyz.volcanobay.cabalist.Cabalist;

public final class SpellRenderStates {
    public static final ResourceLocation BLOOM_FRAMEBUFFER = Cabalist.id("spell_bloom");
    public static final RenderStateShard.OutputStateShard BLOOM_OUTPUT = VeilRenderBridge.outputState(BLOOM_FRAMEBUFFER);
    // Blends like RenderSystem.defaultBlendFunc, which vanilla's translucent shard doesn't for alpha.
    public static final RenderStateShard.TransparencyStateShard DEFAULT_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("cabalist_default_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    private SpellRenderStates() {
    }

    public static RenderStateShard.OutputStateShard output(boolean bloom) {
        return bloom ? BLOOM_OUTPUT : RenderStateShard.MAIN_TARGET;
    }
}
