package xyz.volcanobay.cabalist.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.volcanobay.cabalist.system.barrier.BarrierSystem;

@Mixin(Projectile.class)
public class ProjectileMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void cabalist$deflectOffBarriers(CallbackInfo ci) {
        BarrierSystem.deflect((Projectile) (Object) this);
    }
}
