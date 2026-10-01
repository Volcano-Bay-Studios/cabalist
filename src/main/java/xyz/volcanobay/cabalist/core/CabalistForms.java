package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.content.spell.form.AreaForm;
import xyz.volcanobay.cabalist.content.spell.form.BeamForm;
import xyz.volcanobay.cabalist.content.spell.form.ProjectileForm;
import xyz.volcanobay.cabalist.content.spell.form.SkyStrikeForm;
import xyz.volcanobay.cabalist.content.spell.form.WanderForm;
import xyz.volcanobay.cabalist.system.form.Form;

public class CabalistForms {
    public static final ResourceKey<Registry<Form>> FORM_KEY = ResourceKey.createRegistryKey(Cabalist.id("form"));

    private static final RegistrationProvider<Form> FORMS = RegistrationProvider.get(FORM_KEY, Cabalist.MODID);
    public static final Registry<Form> FORM_REGISTRY = FORMS.asVanillaRegistry();

    public static final RegistryObject<Form> WANDER = FORMS.register("wander", WanderForm::new);
    public static final RegistryObject<Form> PROJECTILE = FORMS.register("projectile", ProjectileForm::new);
    public static final RegistryObject<Form> BEAM = FORMS.register("beam", BeamForm::new);
    public static final RegistryObject<Form> AREA = FORMS.register("area", AreaForm::new);
    public static final RegistryObject<Form> SKY_STRIKE = FORMS.register("sky_strike", SkyStrikeForm::new);

    public static void bootstrap() {
    }
}
