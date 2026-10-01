package xyz.volcanobay.cabalist.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.volcanobay.cabalist.system.barrier.BarrierSystem;

@Mixin(Entity.class)
public class EntityMixin {

    @ModifyReturnValue(method = "collide", at = @At("RETURN"))
    private Vec3 cabalist$stopAtBarriers(Vec3 movement) {
        return BarrierSystem.clip((Entity) (Object) this, movement);
    }
}
