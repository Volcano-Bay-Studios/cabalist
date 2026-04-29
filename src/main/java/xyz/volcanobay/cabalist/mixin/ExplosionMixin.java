package xyz.volcanobay.cabalist.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(Explosion.class)
public class ExplosionMixin {

    @WrapOperation(method = "explode", at = @At(value = "INVOKE", target = "Ljava/util/Optional;get()Ljava/lang/Object;"))
    public Object explode(Optional instance, Operation<Float> original) {
        return instance.get();
    }
}
