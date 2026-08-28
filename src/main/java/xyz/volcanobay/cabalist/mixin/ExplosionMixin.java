package xyz.volcanobay.cabalist.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.volcanobay.cabalist.content.entropy.networks.EntropyNetwork;
import xyz.volcanobay.cabalist.core.CabalistSpatialNetworks;
import xyz.volcanobay.cabalist.system.network.SpatialNetworkMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(Explosion.class)
public class ExplosionMixin {
    @Shadow
    @Final
    private Level level;

    @Shadow
    @Final
    private float radius;

    @Shadow
    @Final
    private double z;

    @Shadow
    @Final
    private double y;

    @Shadow
    @Final
    private double x;

    @Unique
    List<Pair<EntropyNetwork, BlockPos>> cabalist$search;

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    public void explode(CallbackInfo ci) {

        BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
        SpatialNetworkMap<EntropyNetwork> networkMap = CabalistSpatialNetworks.ENTROPY_NETWORK.get(level);
        cabalist$search = new ArrayList<>();
        float radius1 = radius + 2;
        for (int x = (int) (pos.getX() - radius1); x < pos.getX() + radius1; x++) {
            for (int y = (int) (pos.getY() - radius1); y < pos.getY() + radius1; y++) {
                for (int z = (int) (pos.getZ() - radius1); z < pos.getZ() + radius1; z++) {
                    EntropyNetwork network = networkMap.getNetwork(x, y, z);
                    if (network == null) continue;
                    cabalist$search.add(new Pair<>(network, new BlockPos(x, y, z)));
                }
            }
        }
    }

    @WrapOperation(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ExplosionDamageCalculator;getBlockExplosionResistance(Lnet/minecraft/world/level/Explosion;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)Ljava/util/Optional;"))
    public Optional<Float> explode(ExplosionDamageCalculator instance, Explosion explosion, BlockGetter reader, BlockPos pos, BlockState state, FluidState fluid, Operation<Optional<Float>> original, @Local(type = Float.class, ordinal = 0) float power) {
        Optional<Float> blockExplosionResistance = instance.getBlockExplosionResistance(explosion, reader, pos, state, fluid);
        float originalPower = 0;
        if (blockExplosionResistance.isPresent()) {
            originalPower = blockExplosionResistance.get();
        }
        SpatialNetworkMap<EntropyNetwork> networkMap = CabalistSpatialNetworks.ENTROPY_NETWORK.get((Level) reader);
        int radius = (int) (2);
        if (level instanceof ServerLevel serverLevel) {
            if (!cabalist$search.isEmpty() && !reader.getBlockState(pos).isAir()) {
                double entropyToDisperse = power * 0.01;
                double entropyDispersed = 0;
                while (entropyToDisperse > 0.001) {
                    for (Pair<EntropyNetwork, BlockPos> network : cabalist$search) {
                        double amountToTry = ((entropyToDisperse / cabalist$search.size()) / (network.getSecond().distSqr(pos) * 10)) / 100;
                        double dispersed = network.getFirst().addFreeEntropy(amountToTry);
                        entropyToDisperse -= dispersed;
                        entropyDispersed += dispersed * 50000;
                    }
                    entropyToDisperse -= 0.005;
                }
                serverLevel.sendParticles(ParticleTypes.ASH, pos.getX(), pos.getY(), pos.getZ(), (int) power, 0.5, 0.5, 0.5, 0.1);
                return Optional.of((float) entropyDispersed + originalPower);
            } else {
                return blockExplosionResistance;
            }
        }
        return blockExplosionResistance;
    }
}
