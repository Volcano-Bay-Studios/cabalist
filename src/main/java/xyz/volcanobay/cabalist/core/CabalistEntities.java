package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.entity.FallingBeam;
import xyz.volcanobay.cabalist.entity.LifeforceGlyph;
import xyz.volcanobay.cabalist.entity.SpellProjectile;

public class CabalistEntities {
    private static final RegistrationProvider<EntityType<?>> ENTITY_TYPES = RegistrationProvider.get(Registries.ENTITY_TYPE, Cabalist.MODID);

    public static final RegistryObject<EntityType<SpellProjectile>> SPELL_PROJECTILE = ENTITY_TYPES.register("spell_projectile",
            () -> EntityType.Builder.<SpellProjectile>of(SpellProjectile::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f).clientTrackingRange(8).updateInterval(1).build("spell_projectile"));

    public static final RegistryObject<EntityType<FallingBeam>> SKY_STRIKE = ENTITY_TYPES.register("sky_strike",
            () -> EntityType.Builder.<FallingBeam>of(FallingBeam::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(8).updateInterval(1).build("sky_strike"));

    public static final RegistryObject<EntityType<LifeforceGlyph>> LIFEFORCE_GLYPH = ENTITY_TYPES.register("lifeforce_glyph",
            () -> EntityType.Builder.<LifeforceGlyph>of(LifeforceGlyph::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(6).updateInterval(1).build("lifeforce_glyph"));

    public static void bootstrap() {
    }
}
