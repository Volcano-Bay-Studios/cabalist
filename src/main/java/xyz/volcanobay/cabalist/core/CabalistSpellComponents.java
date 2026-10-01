package xyz.volcanobay.cabalist.core;

import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.content.spell.component.*;
import xyz.volcanobay.cabalist.content.spell.component.AspectComponent;
import xyz.volcanobay.cabalist.content.spell.component.ColorComponent;
import xyz.volcanobay.cabalist.content.spell.component.Conjunction;
import xyz.volcanobay.cabalist.content.spell.component.DevotionComponent;
import xyz.volcanobay.cabalist.content.spell.component.DiesComponent;
import xyz.volcanobay.cabalist.content.spell.component.FormComponent;
import xyz.volcanobay.cabalist.content.spell.component.FormSizeComponent;
import xyz.volcanobay.cabalist.content.spell.component.LifeforceOptOut;
import xyz.volcanobay.cabalist.content.spell.component.MembershipComponent;
import xyz.volcanobay.cabalist.content.spell.component.NumberComponent;
import xyz.volcanobay.cabalist.content.spell.component.ReleaseComponent;
import xyz.volcanobay.cabalist.content.spell.component.RenameComponent;
import xyz.volcanobay.cabalist.content.spell.component.RequirementComponent;
import xyz.volcanobay.cabalist.content.spell.component.TargetAnchor;
import xyz.volcanobay.cabalist.content.spell.referent.*;
import xyz.volcanobay.cabalist.system.form.FormDimension;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.SpellComponent;

import java.util.function.Supplier;

public class CabalistSpellComponents {
    public static final ResourceKey<Registry<SpellComponent>> COMPONENT_KEY = ResourceKey.createRegistryKey(Cabalist.id("component"));

    private static final RegistrationProvider<SpellComponent> COMPONENT = RegistrationProvider.get(COMPONENT_KEY, Cabalist.MODID);
    public static final Registry<SpellComponent> PART_REGISTRY = COMPONENT.asVanillaRegistry();

    public static final RegistryObject<SpellComponent> NUMBER = registerPart("number", NumberComponent::new);
    public static final RegistryObject<SpellComponent> ENTROPY_DEVOTION = registerPart("entropy_devotion", () -> new DevotionComponent(CabalistSpellComponents.ENTROPY));
    public static final RegistryObject<SpellComponent> LIFE_DEVOTION = registerPart("life_devotion", () -> new DevotionComponent(CabalistSpellComponents.LIFE));
    public static final RegistryObject<SpellComponent> SOVEREIGN_DEVOTION = registerPart("sovereign_devotion", () -> new DevotionComponent(CabalistSpellComponents.SOVEREIGN));
    public static final RegistryObject<Domain> ENTROPY = registerDomain("entropy");
    public static final RegistryObject<Domain> LIFE = registerDomain("life");
    public static final RegistryObject<Domain> SOVEREIGN = registerDomain("sovereign");
    public static final RegistryObject<Domain> WATER = registerDomain("water");
    public static final RegistryObject<SpellComponent> SELF = registerPart("self", SelfReferent::new);
    public static final RegistryObject<SpellComponent> LOCAL = registerPart("local", LocalReferent::new);
    public static final RegistryObject<SpellComponent> MAIN_HAND = registerPart("main_hand", () -> new ItemInHandReferent(InteractionHand.MAIN_HAND));
    public static final RegistryObject<SpellComponent> OFF_HAND = registerPart("off_hand", () -> new ItemInHandReferent(InteractionHand.OFF_HAND));
    public static final RegistryObject<SpellComponent> ENEMY = registerPart("enemy", EnemyReferent::new);
    public static final RegistryObject<SpellComponent> DISTANCE = registerPart("distance", DistanceReferent::new);
    public static final RegistryObject<SpellComponent> STRIKE = registerPart("strike", TargetAnchor::new);
    public static final RegistryObject<SpellComponent> AND = registerPart("and", () -> new Conjunction(Conjunction.Kind.TOGETHER));
    public static final RegistryObject<SpellComponent> THEN = registerPart("then", () -> new Conjunction(Conjunction.Kind.AFTER));
    public static final RegistryObject<SpellComponent> OR = registerPart("or", () -> new Conjunction(Conjunction.Kind.EITHER));

    public static final RegistryObject<SpellComponent> SPELLS = registerPart("spells", SpellsReferent::new);
    public static final RegistryObject<SpellComponent> CURRENT_SPELL = registerPart("current_spell", CurrentSpellReferent::new);
    public static final RegistryObject<SpellComponent> THIS = registerPart("this", ThisReferent::new);
    public static final RegistryObject<SpellComponent> LITERAL = registerPart("literal", LiteralReferent::new);
    public static final RegistryObject<SpellComponent> DISPEL = registerPart("dispel", () -> new AspectComponent(CabalistAspects.DISPEL));

    public static final RegistryObject<SpellComponent> BLOOD = registerPart("blood", BloodReferent::new);
    public static final RegistryObject<SpellComponent> CONTRACT = registerPart("contract", ContractReferent::new);
    public static final RegistryObject<SpellComponent> JOIN_CONTRACT = registerPart("join_contract", () -> new MembershipComponent(CabalistAspects.JOIN_CONTRACT));
    public static final RegistryObject<SpellComponent> LEAVE_CONTRACT = registerPart("leave_contract", () -> new AspectComponent(CabalistAspects.LEAVE_CONTRACT));
    public static final RegistryObject<SpellComponent> OPEN_DRAFT = registerPart("open_draft", () -> new AspectComponent(CabalistAspects.OPEN_DRAFT));
    public static final RegistryObject<SpellComponent> ACCEPT_CHANGES = registerPart("accept_changes", () -> new AspectComponent(CabalistAspects.ACCEPT_CHANGES));
    public static final RegistryObject<SpellComponent> APPRAISE = registerPart("appraise", () -> new AspectComponent(CabalistAspects.APPRAISE));
    public static final RegistryObject<SpellComponent> HIDE = registerPart("hide", () -> new AspectComponent(CabalistAspects.HIDE));
    public static final RegistryObject<SpellComponent> PROTECT = registerPart("protect", () -> new AspectComponent(CabalistAspects.PROTECT));
    public static final RegistryObject<SpellComponent> BARRIER = registerPart("barrier", () -> new AspectComponent(CabalistAspects.BARRIER));
    public static final RegistryObject<SpellComponent> REBIND = registerPart("rebind", () -> new AspectComponent(CabalistAspects.REBIND));
    public static final RegistryObject<SpellComponent> RENAME_CONTRACT = registerPart("rename_contract", RenameComponent::new);
    public static final RegistryObject<SpellComponent> WORLD = registerPart("world", WorldReferent::new);
    public static final RegistryObject<SpellComponent> INVITE = registerPart("invite", () -> new MembershipComponent(CabalistAspects.INVITE));
    public static final RegistryObject<SpellComponent> APPOINT = registerPart("appoint", () -> new MembershipComponent(CabalistAspects.APPOINT));
    public static final RegistryObject<SpellComponent> EXPEL = registerPart("expel", () -> new MembershipComponent(CabalistAspects.EXPEL));
    public static final RegistryObject<SpellComponent> CONSENT = registerPart("consent", () -> new AspectComponent(CabalistAspects.CONSENT));
    public static final RegistryObject<SpellComponent> DENY = registerPart("deny", () -> new AspectComponent(CabalistAspects.DENY));

    public static final RegistryObject<SpellComponent> WHEN_TOUCHED = registerPart("when_touched", () -> new RequirementComponent(CabalistTerms.TOUCHED));
    public static final RegistryObject<SpellComponent> WHEN_STEPPED_ON = registerPart("when_stepped_on", () -> new RequirementComponent(CabalistTerms.STEPPED_ON));
    public static final RegistryObject<SpellComponent> WHEN_STRUCK = registerPart("when_struck", () -> new RequirementComponent(CabalistTerms.STRUCK));
    public static final RegistryObject<SpellComponent> WHEN_STRIKING = registerPart("when_striking", () -> new RequirementComponent(CabalistTerms.STRIKES));
    public static final RegistryObject<SpellComponent> WHEN_BLOODIED = registerPart("when_bloodied", () -> new RequirementComponent(CabalistTerms.BLOODIED));
    public static final RegistryObject<SpellComponent> WHEN_DIES = registerPart("when_dies", () -> new RequirementComponent(CabalistTerms.DIES));
    public static final RegistryObject<SpellComponent> WHEN_RELEASED = registerPart("when_released", () -> new RequirementComponent(CabalistTerms.RELEASED));
    public static final RegistryObject<SpellComponent> DIES = registerPart("dies", DiesComponent::new);

    public static final RegistryObject<SpellComponent> IGNITE = registerPart("ignite", () -> new AspectComponent(CabalistAspects.IGNITE));
    public static final RegistryObject<SpellComponent> SCORCH = registerPart("scorch", () -> new AspectComponent(CabalistAspects.SCORCH));
    public static final RegistryObject<SpellComponent> HARM = registerPart("harm", () -> new AspectComponent(CabalistAspects.HARM));
    public static final RegistryObject<SpellComponent> SACRIFICE = registerPart("sacrifice", () -> new AspectComponent(CabalistAspects.SACRIFICE));
    public static final RegistryObject<SpellComponent> HEAL = registerPart("heal", () -> new AspectComponent(CabalistAspects.HEAL));
    public static final RegistryObject<SpellComponent> IMBUE = registerPart("imbue", () -> new AspectComponent(CabalistAspects.IMBUE));
    public static final RegistryObject<SpellComponent> FREEZE = registerPart("freeze", () -> new AspectComponent(CabalistAspects.FREEZE));
    public static final RegistryObject<SpellComponent> MATERIALIZE = registerPart("materialize", () -> new AspectComponent(CabalistAspects.MATERIALIZE));
    public static final RegistryObject<SpellComponent> SIPHON = registerPart("siphon", () -> new AspectComponent(CabalistAspects.SIPHON));

    public static final RegistryObject<SpellComponent> PROJECTILE = registerPart("projectile", () -> new FormComponent(CabalistForms.PROJECTILE));
    public static final RegistryObject<SpellComponent> BEAM = registerPart("beam", () -> new FormComponent(CabalistForms.BEAM));
    public static final RegistryObject<SpellComponent> AREA = registerPart("area", () -> new FormComponent(CabalistForms.AREA));
    public static final RegistryObject<SpellComponent> WITHOUT_LIFE = registerPart("without_life", LifeforceOptOut::new);
    public static final RegistryObject<SpellComponent> RELEASE = registerPart("release", ReleaseComponent::new);
    public static final RegistryObject<SpellComponent> LONG = registerPart("long", () -> new FormSizeComponent(FormDimension.SIZE, 2));
    public static final RegistryObject<SpellComponent> SHORT = registerPart("short", () -> new FormSizeComponent(FormDimension.SIZE, 0.5));
    public static final RegistryObject<SpellComponent> WIDE = registerPart("wide", () -> new FormSizeComponent(FormDimension.BREADTH, 2));
    public static final RegistryObject<SpellComponent> NARROW = registerPart("narrow", () -> new FormSizeComponent(FormDimension.BREADTH, 0.5));
    public static final RegistryObject<SpellComponent> TALL = registerPart("tall", () -> new FormSizeComponent(FormDimension.HEIGHT, 2));
    public static final RegistryObject<SpellComponent> LOW = registerPart("low", () -> new FormSizeComponent(FormDimension.HEIGHT, 0.5));
    public static final RegistryObject<SpellComponent> SKY_STRIKE = registerPart("sky_strike", () -> new FormComponent(CabalistForms.SKY_STRIKE));
    public static final RegistryObject<SpellComponent> RED = registerPart("color_red", () -> new ColorComponent(0xFF3A2A));
    public static final RegistryObject<SpellComponent> ORANGE = registerPart("color_orange", () -> new ColorComponent(0xFF8A1F));
    public static final RegistryObject<SpellComponent> YELLOW = registerPart("color_yellow", () -> new ColorComponent(0xFFE83A));
    public static final RegistryObject<SpellComponent> GOLD = registerPart("color_gold", () -> new ColorComponent(0xFFC94A));
    public static final RegistryObject<SpellComponent> GREEN = registerPart("color_green", () -> new ColorComponent(0x4CFF5A));
    public static final RegistryObject<SpellComponent> CYAN = registerPart("color_cyan", () -> new ColorComponent(0x3AF2FF));
    public static final RegistryObject<SpellComponent> BLUE = registerPart("color_blue", () -> new ColorComponent(0x3A6BFF));
    public static final RegistryObject<SpellComponent> PURPLE = registerPart("color_purple", () -> new ColorComponent(0xB38CFF));
    public static final RegistryObject<SpellComponent> PINK = registerPart("color_pink", () -> new ColorComponent(0xFF6AD5));
    public static final RegistryObject<SpellComponent> WHITE = registerPart("color_white", () -> new ColorComponent(0xFFFFFF));
    public static final RegistryObject<SpellComponent> BLACK = registerPart("color_black", () -> new ColorComponent(0xFF111111));

    private static RegistryObject<Domain> registerDomain(String name) {
        return COMPONENT.register(Cabalist.id(name), Domain::new);
    }

    private static RegistryObject<SpellComponent> registerPart(String name, Supplier<SpellComponent> termFunction) {
        ResourceLocation location = Cabalist.id(name);
        return COMPONENT.register(location, termFunction);
    }

    public static void bootstrap() {
    }
}
