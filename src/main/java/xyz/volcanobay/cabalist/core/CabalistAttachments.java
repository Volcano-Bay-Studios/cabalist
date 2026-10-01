package xyz.volcanobay.cabalist.core;

import com.mojang.serialization.Codec;
import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.system.contract.ContractMemberships;
import xyz.volcanobay.cabalist.system.energy.EnergyStack;
import xyz.volcanobay.cabalist.system.energy.Lifeforce;
import xyz.volcanobay.cabalist.system.spell.HangingSpellsData;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;

public class CabalistAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Cabalist.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EntitySubject>> ENTITY_SUBJECT = ATTACHMENT_TYPES.register("entity_subject",
            () -> AttachmentType.builder(holder -> new EntitySubject((Entity) holder)).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EnergyStack>> ENERGY_STACK = ATTACHMENT_TYPES.register("energy_stack",
            () -> AttachmentType.serializable(EnergyStack::new).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<HangingSpellsData>> HANGING_SPELLS = ATTACHMENT_TYPES.register("hanging_spells",
            () -> AttachmentType.serializable(HangingSpellsData::new).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ContractMemberships>> CONTRACT_MEMBERSHIPS = ATTACHMENT_TYPES.register("contract_memberships",
            () -> AttachmentType.builder(ContractMemberships::new).serialize(ContractMemberships.CODEC).copyOnDeath().build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Lifeforce.Data>> LIFEFORCE = ATTACHMENT_TYPES.register("lifeforce",
            () -> AttachmentType.builder(() -> new Lifeforce.Data(0, 0)).serialize(Lifeforce.Data.CODEC).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> WORLD_CONSENT = ATTACHMENT_TYPES.register("world_consent",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    public static void bootstrap(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
