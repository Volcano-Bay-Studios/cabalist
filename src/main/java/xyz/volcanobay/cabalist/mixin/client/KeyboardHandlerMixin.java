package xyz.volcanobay.cabalist.mixin.client;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.volcanobay.cabalist.client.casting.ClientCastingSystem;
import xyz.volcanobay.cabalist.client.request.RequestReading;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void cabalist$writeSpell(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (ClientCastingSystem.INSTANCE.onKey(window, key, action) || RequestReading.INSTANCE.onKey(window, key, action)) {
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void cabalist$typeSpell(long window, int codePoint, int modifiers, CallbackInfo ci) {
        if (ClientCastingSystem.INSTANCE.onChar(window, codePoint) || RequestReading.INSTANCE.onChar(window, codePoint)) {
            ci.cancel();
        }
    }
}
