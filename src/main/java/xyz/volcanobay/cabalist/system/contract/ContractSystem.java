package xyz.volcanobay.cabalist.system.contract;

import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.UsernameCache;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.core.CabalistDataComponents;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.networking.packet.HangingSpellsS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.InscribedSpellsS2CPacket;
import xyz.volcanobay.cabalist.system.entropy.EntropyNetworkContract;
import xyz.volcanobay.cabalist.system.render.SpellVisuals;
import xyz.volcanobay.cabalist.system.request.RequestSystem;
import xyz.volcanobay.cabalist.system.spell.PendingSpell;
import xyz.volcanobay.cabalist.system.spell.SpellPayment;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.ItemSubject;
import xyz.volcanobay.cabalist.system.subject.RuneSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ContractSystem {
    public static final ContractSystem INSTANCE = new ContractSystem();
    private static final int SYNC_INTERVAL_TICKS = 10;
    private static final int RESYNC_INTERVAL_TICKS = 100;
    private static final float CHARGE_STEPS = 256;

    private final Map<UUID, Contract> contracts = new LinkedHashMap<>();
    private final Map<Object, List<HangingSpellsS2CPacket.Entry>> synced = new HashMap<>();
    private final Map<Object, SyncTarget> targets = new HashMap<>();
    private final Map<Object, SyncTarget> lastTargets = new HashMap<>();

    private record BlockHost(Level level, BlockPos pos) {
    }

    private record SyncTarget(Optional<BlockPos> block, ServerLevel level, int entityId, @Nullable Entity entity) {
    }

    private ContractSystem() {}

    public void addContract(Contract contract) {
        contracts.put(contract.getUUID(), contract);
    }

    public Contract create(String name, @Nullable UUID creator) {
        Contract contract = new Contract(name);
        contract.setCreatorId(creator);
        if (creator != null) {
            contract.addMemberId(creator);
        }
        addContract(contract);
        contract.logAction(creator, "create", "", "accepted");
        return contract;
    }

    public void removeContract(Contract contract) {
        contracts.remove(contract.getUUID());
        contract.getPendingSpells().forEach(PendingSpell::release);
        RequestSystem.INSTANCE.removeFor(contract);
        DraftSystem.INSTANCE.removeFor(contract);
    }

    public void destroyContract(UUID contractId, String reason) {
        Contract contract = contracts.get(contractId);
        if (contract != null) {
            contract.logAction(null, "destroy", reason, "accepted");
            removeContract(contract);
        }
    }

    public Collection<Contract> getContracts() {
        return contracts.values();
    }

    public @Nullable Contract getContract(UUID uuid) {
        return contracts.get(uuid);
    }

    public @Nullable Contract getContract(String name) {
        for (Contract contract : contracts.values()) {
            if (name.equals(contract.getName())) {
                return contract;
            }
        }
        return null;
    }

    public @Nullable Contract getContract(ItemStack stack) {
        UUID contractId = stack.get(CabalistDataComponents.CONTRACT.get());
        return contractId == null ? null : contracts.get(contractId);
    }

    public @Nullable Contract getNetworkContract(ResourceKey<Level> dimension, int networkId) {
        for (Contract contract : contracts.values()) {
            if (contract.isForNetwork(dimension, networkId)) {
                return contract;
            }
        }
        return null;
    }

    public @Nullable Contract getNetworkAuthority(@Nullable Level level, int networkId) {
        if (networkId < 0) {
            return null;
        }
        for (Contract contract : contracts.values()) {
            if (contract.getNetworkId() == networkId && contract.getLevel() == level && contract.getSettings().isAuthorizing()) {
                return contract;
            }
        }
        return null;
    }

    public String getContracteeName(@Nullable UUID id) {
        if (id == null) {
            return "none";
        }
        if (id.equals(WorldContractee.WORLD_ID)) {
            return "the world";
        }
        Contract contract = contracts.get(id);
        if (contract != null) {
            return contract.getShownName();
        }
        String name = UsernameCache.getLastKnownUsername(id);
        return name != null ? name : id.toString().substring(0, 8);
    }

    public void tick(MinecraftServer server) {
        for (Contract contract : contracts.values()) {
            contract.tick(server);
        }
        if (server.getTickCount() % SYNC_INTERVAL_TICKS == 0) {
            syncInscribed(server.getTickCount() % RESYNC_INTERVAL_TICKS == 0);
        }
    }

    private void syncInscribed(boolean resendAll) {
        Map<Object, List<HangingSpellsS2CPacket.Entry>> current = new HashMap<>();
        targets.clear();
        for (Contract contract : contracts.values()) {
            Object key = getSyncKey(contract.getHost());
            if (key == null) {
                continue;
            }
            List<HangingSpellsS2CPacket.Entry> spells = current.computeIfAbsent(key, k -> new ArrayList<>());
            double stored = contract.getEnergyStack().get(CabalistEnergyTypes.ENTROPY.get());
            for (PendingSpell pending : contract.getPendingSpells()) {
                double cost = SpellPayment.INSTANCE.estimateCost(pending.getSpell());
                float charge = cost <= 0 ? 1 : (float) Math.min(1, stored / cost);
                charge = Math.round(charge * CHARGE_STEPS) / CHARGE_STEPS;
                spells.add(HangingSpellsS2CPacket.Entry.of(pending, SpellVisuals.getPalette(pending.getSpell()), charge).withArbiter(contract.getCreatorId()));
            }
        }
        for (Map.Entry<Object, List<HangingSpellsS2CPacket.Entry>> entry : current.entrySet()) {
            if (resendAll || !entry.getValue().equals(synced.get(entry.getKey()))) {
                send(targets.get(entry.getKey()), entry.getValue());
            }
        }
        for (Iterator<Map.Entry<Object, List<HangingSpellsS2CPacket.Entry>>> iterator = synced.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Object, List<HangingSpellsS2CPacket.Entry>> entry = iterator.next();
            if (!current.containsKey(entry.getKey())) {
                SyncTarget gone = lastTargets.get(entry.getKey());
                if (gone != null && (gone.entity() == null || !gone.entity().isRemoved())) {
                    send(gone, List.of());
                }
                iterator.remove();
                lastTargets.remove(entry.getKey());
            }
        }
        synced.putAll(current);
        lastTargets.putAll(targets);
    }

    private @Nullable Object getSyncKey(@Nullable Subject host) {
        if (host instanceof RuneSubject rune && rune.isValid() && rune.getLevel() instanceof ServerLevel level) {
            BlockHost key = new BlockHost(level, rune.getPos());
            targets.put(key, new SyncTarget(Optional.of(rune.getPos()), level, -1, null));
            return key;
        }
        if (host instanceof ItemSubject item && item.isValid() && item.getHolder() instanceof EntitySubject holder
                && holder.getEntity() instanceof LivingEntity living && holder.getLevel() instanceof ServerLevel level
                && (living.getMainHandItem() == item.getStack() || living.getOffhandItem() == item.getStack())) {
            Integer key = living.getId();
            targets.put(key, new SyncTarget(Optional.empty(), level, living.getId(), living));
            return key;
        }
        return null;
    }

    private static void send(SyncTarget target, List<HangingSpellsS2CPacket.Entry> spells) {
        InscribedSpellsS2CPacket packet = new InscribedSpellsS2CPacket(target.block(), target.entityId(), spells);
        if (target.block().isPresent()) {
            VeilPacketManager.tracking(target.level(), target.block().get()).sendPacket(packet);
        } else {
            VeilPacketManager.trackingAndSelf(target.entity()).sendPacket(packet);
        }
    }

    public void clear() {
        contracts.clear();
        synced.clear();
        targets.clear();
        lastTargets.clear();
    }

    public CompoundTag write(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Contract contract : contracts.values()) {
            list.add(contract.write(new CompoundTag()));
        }
        tag.put("contracts", list);
        return tag;
    }

    public void read(CompoundTag tag) {
        contracts.clear();
        ListTag list = tag.getList("contracts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag contractTag = list.getCompound(i);
            Contract contract = EntropyNetworkContract.TYPE.equals(contractTag.getString("type")) ? new EntropyNetworkContract() : new Contract();
            contract.read(contractTag);
            addContract(contract);
        }
    }
}
