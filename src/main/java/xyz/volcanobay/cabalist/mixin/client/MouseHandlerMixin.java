package xyz.volcanobay.cabalist.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.volcanobay.cabalist.client.casting.ClientCastingSystem;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void cabalist$slowTurnWhileCasting(LocalPlayer player, double yaw, double pitch, Operation<Void> original) {
        double scale = ClientCastingSystem.INSTANCE.onTurn(yaw, pitch);
        original.call(player, yaw * scale, pitch * scale);
    }
}
