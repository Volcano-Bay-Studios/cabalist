package xyz.volcanobay.cabalist.core;

import com.mojang.serialization.MapCodec;
import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.particle.GlyphParticleOptions;

public class CabalistParticles {
    private static final RegistrationProvider<ParticleType<?>> PARTICLE_TYPES = RegistrationProvider.get(Registries.PARTICLE_TYPE, Cabalist.MODID);

    public static final RegistryObject<ParticleType<GlyphParticleOptions>> GLYPH = PARTICLE_TYPES.register("glyph", () -> new ParticleType<>(false) {
        @Override
        public @NotNull MapCodec<GlyphParticleOptions> codec() {
            return GlyphParticleOptions.CODEC;
        }

        @Override
        public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, GlyphParticleOptions> streamCodec() {
            return GlyphParticleOptions.STREAM_CODEC;
        }
    });

    public static void bootstrap() {
    }
}
