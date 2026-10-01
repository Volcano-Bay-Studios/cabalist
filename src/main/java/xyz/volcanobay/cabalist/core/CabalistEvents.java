package xyz.volcanobay.cabalist.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.block.ContractSlateBlock;
import xyz.volcanobay.cabalist.blockentity.ContractBlockEntity;
import xyz.volcanobay.cabalist.entity.LifeforceGlyph;
import xyz.volcanobay.cabalist.persistent.ContractSavedData;
import xyz.volcanobay.cabalist.persistent.NetworkSavedData;
import xyz.volcanobay.cabalist.system.barrier.BarrierSystem;
import xyz.volcanobay.cabalist.system.blood.BloodHelper;
import xyz.volcanobay.cabalist.system.casting.CastingSystem;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.contract.DraftSystem;
import xyz.volcanobay.cabalist.system.contract.InscriptionSync;
import xyz.volcanobay.cabalist.system.energy.Lifeforce;
import xyz.volcanobay.cabalist.system.energy.MembersEnergyStack;
import xyz.volcanobay.cabalist.system.focus.LifeforceCaps;
import xyz.volcanobay.cabalist.system.focus.LifeforceFlow;
import xyz.volcanobay.cabalist.system.form.DeliverySystem;
import xyz.volcanobay.cabalist.system.network.Network;
import xyz.volcanobay.cabalist.system.protection.ProtectionSystem;
import xyz.volcanobay.cabalist.system.request.NoticeSystem;
import xyz.volcanobay.cabalist.system.request.RequestSystem;
import xyz.volcanobay.cabalist.system.spell.HangingSpellSystem;
import xyz.volcanobay.cabalist.system.spell.SpellTriggers;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.ItemSubject;
import xyz.volcanobay.cabalist.system.visibility.HidingSystem;

import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = Cabalist.MODID)
public class CabalistEvents {
    private static final int INSCRIPTION_SYNC_TICKS = 20;
    private static final int PROTECTION_PRUNE_TICKS = 200;
    private static final int FOCUS_BALANCE_TICKS = 10;

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        ContractSavedData.load(event.getServer());
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(CabalistSpellDictionary.Reloader.INSTANCE);
        event.addListener(CabalistRequestStyles.Reloader.INSTANCE);
        event.addListener(CabalistRenderSpecs.Reloader.INSTANCE);
    }

    @SubscribeEvent
    public static void onServerPreTick(ServerTickEvent.Pre event) {
        ContractSystem.INSTANCE.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        HangingSpellSystem.INSTANCE.tick(server);
        DeliverySystem.INSTANCE.tick();
        BarrierSystem.SERVER.tickServer(server);
        CastingSystem.INSTANCE.tick(server);
        NoticeSystem.INSTANCE.tick();
        RequestSystem.INSTANCE.tick(server);
        DraftSystem.INSTANCE.tick(server);
        HidingSystem.INSTANCE.tick(server);
        if (server.getTickCount() % PROTECTION_PRUNE_TICKS == 0) {
            server.getAllLevels().forEach(ProtectionSystem.INSTANCE::prune);
        }
    }

    @SubscribeEvent
    public static void onServerLevelTick(LevelTickEvent.Pre event) {
        if (event.getLevel() instanceof ServerLevel level) {
            NetworkSavedData.get(level);
            for (CabalistSpatialNetworks.NetworkHolder<? extends Network> network : CabalistSpatialNetworks.NETWORKS) {
                network.get(level).tick(level);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (CabalistSpatialNetworks.NetworkHolder<? extends Network> network : CabalistSpatialNetworks.NETWORKS) {
            network.wipe();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ContractSystem.INSTANCE.clear();
        DraftSystem.INSTANCE.clear();
        RequestSystem.INSTANCE.clear();
        NoticeSystem.INSTANCE.clear();
        HangingSpellSystem.INSTANCE.clear();
        DeliverySystem.INSTANCE.clear();
        CastingSystem.INSTANCE.clear();
        HidingSystem.INSTANCE.clear();
        ProtectionSystem.INSTANCE.clear();
        BarrierSystem.SERVER.clear();
        BarrierSystem.CLIENT.clear();
        MembersEnergyStack.clear();
    }

    @SubscribeEvent
    public static void onLivingDamaged(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        Lifeforce.settle(victim);
        Entity attacker = event.getSource().getEntity();
        EntitySubject victimSubject = EntitySubject.of(victim);
        EntitySubject cause = attacker == null ? null : EntitySubject.of(attacker);
        SpellTriggers.raise(CabalistTerms.STRUCK.getId(), victimSubject, cause);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack worn = victim.getItemBySlot(slot);
            if (worn.has(CabalistDataComponents.CONTRACT.get())) {
                SpellTriggers.raise(CabalistTerms.STRUCK.getId(), new ItemSubject(worn, victimSubject), cause);
            }
        }
        if (attacker == null) {
            return;
        }
        EntitySubject attackerSubject = EntitySubject.of(attacker);
        SpellTriggers.raise(CabalistTerms.STRIKES.getId(), attackerSubject, victimSubject);
        if (!(attacker instanceof LivingEntity livingAttacker) || event.getSource().getDirectEntity() != attacker) {
            return;
        }
        ItemStack weapon = livingAttacker.getMainHandItem();
        for (ItemStack held : List.of(weapon, livingAttacker.getOffhandItem())) {
            if (held.has(CabalistDataComponents.CONTRACT.get())) {
                SpellTriggers.raise(CabalistTerms.STRIKES.getId(), new ItemSubject(held, attackerSubject), victimSubject);
            }
        }
        BloodHelper.coverInBlood(weapon, livingAttacker, victim, false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        Entity killer = event.getSource().getEntity();
        EntitySubject victimSubject = EntitySubject.of(victim);
        EntitySubject cause = killer == null ? null : EntitySubject.of(killer);
        SpellTriggers.raiseEverywhere(CabalistTerms.DIES.getId(), victimSubject, cause);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack carried = victim.getItemBySlot(slot);
            if (carried.has(CabalistDataComponents.CONTRACT.get())) {
                SpellTriggers.raiseEverywhere(CabalistTerms.DIES.getId(), new ItemSubject(carried, victimSubject), cause);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKilled(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || !(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        ResourceLocation type = LifeforceCaps.getType(victim);
        double fraction = Mth.lerp(level.random.nextDouble(), CabalistConfig.GLYPH_DROP_MIN_FRACTION.get(), CabalistConfig.GLYPH_DROP_MAX_FRACTION.get());
        double amount = LifeforceCaps.get(type) * fraction;
        if (amount > 0) {
            LifeforceGlyph.scatter(level, victim.position().add(0, victim.getBbHeight() / 2, 0), type, amount, false);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && ProtectionSystem.INSTANCE.blocks(level, event.getPos(), event.getPlayer())) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getState().getBlock() instanceof ContractSlateBlock)
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof ContractBlockEntity holder)) {
            return;
        }
        Contract contract = holder.getContract();
        if (contract != null && !contract.isArbiter(EntitySubject.of(event.getPlayer()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level && ProtectionSystem.INSTANCE.blocks(level, event.getPos(), event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (ProtectionSystem.INSTANCE.blocks(event.getLevel(), event.getPos(), event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Entity source = event.getExplosion().getIndirectSourceEntity();
        event.getAffectedBlocks().removeIf(pos -> ProtectionSystem.INSTANCE.blocksExplosion(event.getLevel(), pos, source));
    }

    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof Level level && ProtectionSystem.INSTANCE.isProtected(level, event.getPos())) {
            event.setNewState(event.getOriginalState());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RequestSystem.INSTANCE.onLogin(player);
            BarrierSystem.SERVER.sendTo(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BarrierSystem.SERVER.sendTo(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!event.isEndConquered() && !event.getEntity().level().isClientSide) {
            Lifeforce.set(event.getEntity(), 0);
        }
    }

    @SubscribeEvent
    public static void onItemUsed(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (event.getLevel().isClientSide || !stack.has(CabalistDataComponents.CONTRACT.get())) {
            return;
        }
        EntitySubject holder = EntitySubject.of(event.getEntity());
        SpellTriggers.raise(CabalistTerms.TOUCHED.getId(), new ItemSubject(stack, holder), holder);
    }

    @SubscribeEvent
    public static void onItemPickedUp(ItemEntityPickupEvent.Post event) {
        ItemStack stack = event.getOriginalStack();
        Player player = event.getPlayer();
        if (player.level().isClientSide || !stack.has(CabalistDataComponents.CONTRACT.get())) {
            return;
        }
        EntitySubject holder = EntitySubject.of(player);
        SpellTriggers.raise(CabalistTerms.TOUCHED.getId(), new ItemSubject(stack, holder), holder);
    }

    @SubscribeEvent
    public static void onEntityJoined(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!event.getLevel().isClientSide && entity.hasData(CabalistAttachments.HANGING_SPELLS.get())) {
            entity.getData(CabalistAttachments.HANGING_SPELLS.get()).restore();
        }
    }

    @SubscribeEvent
    public static void onItemEntityRemoved(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }
        Entity.RemovalReason reason = itemEntity.getRemovalReason();
        ItemStack stack = itemEntity.getItem();
        if (reason == null || !reason.shouldDestroy() || stack.isEmpty()) {
            return;
        }
        UUID contractId = stack.get(CabalistDataComponents.CONTRACT.get());
        if (contractId != null) {
            ContractSystem.INSTANCE.destroyContract(contractId, "item destroyed");
        }
    }

    @SubscribeEvent
    public static void onItemBroken(PlayerDestroyItemEvent event) {
        UUID contractId = event.getOriginal().get(CabalistDataComponents.CONTRACT.get());
        if (contractId != null && !event.getEntity().level().isClientSide) {
            ContractSystem.INSTANCE.destroyContract(contractId, "item broken");
        }
    }

    @SubscribeEvent
    public static void onEntityTouched(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        SpellTriggers.raise(CabalistTerms.TOUCHED.getId(), EntitySubject.of(event.getTarget()), EntitySubject.of(event.getEntity()));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        if (player.tickCount % INSCRIPTION_SYNC_TICKS == 0) {
            InscriptionSync.refresh(player);
        }
        if (player.tickCount % FOCUS_BALANCE_TICKS == 0) {
            LifeforceFlow.balance(player);
        }
        for (ItemStack held : List.of(player.getMainHandItem(), player.getOffhandItem())) {
            if (!held.has(CabalistDataComponents.CONTRACT.get())) {
                continue;
            }
            ItemSubject host = new ItemSubject(held, EntitySubject.of(player));
            Contract contract = host.getBoundContract();
            if (contract != null) {
                contract.bindHost(host);
            }
        }
    }
}
