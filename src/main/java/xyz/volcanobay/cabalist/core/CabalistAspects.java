package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.ModConfigSpec;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.content.spell.aspect.AcceptChangesAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.AppointAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.AppraiseAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.BarrierAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.ConsentAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.DispelAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.FreezeAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.HarmAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.HealAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.HideAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.IgniteAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.ImbueAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.JoinContractAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.LeaveContractAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.MaterializeAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.MembershipAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.OpenDraftAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.ProtectAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.RebindAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.RecolorAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.ReleaseAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.RenameContractAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.SacrificeAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.ScorchAspect;
import xyz.volcanobay.cabalist.content.spell.aspect.SiphonAspect;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.request.RequestKind;

import java.util.function.Supplier;

public class CabalistAspects {
    public static final ResourceKey<Registry<Aspect>> ASPECT_KEY = ResourceKey.createRegistryKey(Cabalist.id("aspect"));

    private static final RegistrationProvider<Aspect> ASPECTS = RegistrationProvider.get(ASPECT_KEY, Cabalist.MODID);
    public static final Registry<Aspect> ASPECT_REGISTRY = ASPECTS.asVanillaRegistry();
    private static final ModConfigSpec.Builder CONFIG_BUILDER = new ModConfigSpec.Builder();

    public static final RegistryObject<Aspect> IGNITE = register("ignite", IgniteAspect::new);
    public static final RegistryObject<Aspect> SCORCH = register("scorch", ScorchAspect::new);
    public static final RegistryObject<HarmAspect> HARM = register("harm", HarmAspect::new);
    public static final RegistryObject<Aspect> SACRIFICE = register("sacrifice", SacrificeAspect::new);
    public static final RegistryObject<Aspect> HEAL = register("heal", HealAspect::new);
    public static final RegistryObject<Aspect> IMBUE = register("imbue", ImbueAspect::new);
    public static final RegistryObject<Aspect> FREEZE = register("freeze", FreezeAspect::new);
    public static final RegistryObject<Aspect> SIPHON = register("siphon", SiphonAspect::new);
    public static final RegistryObject<Aspect> MATERIALIZE = register("materialize", MaterializeAspect::new);
    public static final RegistryObject<Aspect> DISPEL = register("dispel", DispelAspect::new);
    public static final RegistryObject<Aspect> JOIN_CONTRACT = register("join_contract", JoinContractAspect::new);
    public static final RegistryObject<Aspect> LEAVE_CONTRACT = register("leave_contract", LeaveContractAspect::new);
    public static final RegistryObject<Aspect> RENAME_CONTRACT = register("rename_contract", RenameContractAspect::new);
    public static final RegistryObject<Aspect> INVITE = register("invite", () -> new MembershipAspect(RequestKind.JOIN));
    public static final RegistryObject<Aspect> EXPEL = register("expel", () -> new MembershipAspect(RequestKind.LEAVE));
    public static final RegistryObject<Aspect> OPEN_DRAFT = register("open_draft", OpenDraftAspect::new);
    public static final RegistryObject<Aspect> ACCEPT_CHANGES = register("accept_changes", AcceptChangesAspect::new);
    public static final RegistryObject<Aspect> APPOINT = register("appoint", AppointAspect::new);
    public static final RegistryObject<Aspect> APPRAISE = register("appraise", AppraiseAspect::new);
    public static final RegistryObject<Aspect> HIDE = register("hide", HideAspect::new);
    public static final RegistryObject<Aspect> PROTECT = register("protect", ProtectAspect::new);
    public static final RegistryObject<Aspect> BARRIER = register("barrier", BarrierAspect::new);
    public static final RegistryObject<Aspect> RECOLOR = register("recolor", RecolorAspect::new);
    public static final RegistryObject<Aspect> REBIND = register("rebind", RebindAspect::new);
    public static final RegistryObject<Aspect> RELEASE = register("release", ReleaseAspect::new);
    public static final RegistryObject<Aspect> CONSENT = register("consent", () -> new ConsentAspect(true));
    public static final RegistryObject<Aspect> DENY = register("deny", () -> new ConsentAspect(false));

    public static final ModConfigSpec CONFIG = CONFIG_BUILDER.build();

    private static <A extends Aspect> RegistryObject<A> register(String name, Supplier<A> factory) {
        A aspect = factory.get();
        CONFIG_BUILDER.push(name);
        aspect.defineConfig(CONFIG_BUILDER);
        CONFIG_BUILDER.pop();
        return ASPECTS.register(name, () -> aspect);
    }

    public static void bootstrap() {
    }
}
