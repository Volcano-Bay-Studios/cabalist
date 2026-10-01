package xyz.volcanobay.cabalist.system.contract;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.block.RuneBlock;
import xyz.volcanobay.cabalist.blockentity.ContractBlockEntity;
import xyz.volcanobay.cabalist.core.CabalistBlocks;
import xyz.volcanobay.cabalist.core.CabalistDataComponents;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.ItemSubject;
import xyz.volcanobay.cabalist.system.subject.RuneSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.util.EntityHelper;

import java.util.UUID;

public record HostTarget(@Nullable UUID entity, @Nullable ResourceKey<Level> dimension, @Nullable BlockPos pos, String label) {

    public static @Nullable HostTarget of(Subject subject) {
        if (subject instanceof ItemSubject item) {
            return of(item.getHolder());
        }
        if (subject instanceof EntitySubject entity) {
            return new HostTarget(entity.getEntity().getUUID(), null, null, entity.getEntity().getName().getString());
        }
        Level level = subject.getLevel();
        if (level == null || subject instanceof Contract) {
            return null;
        }
        Vector3d position = new Vector3d();
        subject.getPosition(position);
        BlockPos pos = BlockPos.containing(position.x, position.y, position.z);
        return new HostTarget(null, level.dimension(), pos, pos.toShortString());
    }

    public String describe() {
        return label;
    }

    public @Nullable UUID getConsentingParty() {
        return entity;
    }

    public boolean move(Contract contract, MinecraftServer server) {
        Subject oldHost = contract.getHost();
        if (oldHost == null || !canRelease(oldHost)) {
            return false;
        }
        boolean isPlaced = entity != null ? placeOnEntity(server, contract) : placeOnBlock(server, contract);
        if (!isPlaced) {
            return false;
        }
        release(oldHost);
        contract.unbindHost(oldHost);
        return true;
    }

    public boolean place(Contract contract, MinecraftServer server) {
        return entity != null ? placeOnEntity(server, contract) : placeOnBlock(server, contract);
    }

    private static boolean canRelease(Subject host) {
        return host instanceof ItemSubject || host instanceof RuneSubject;
    }

    private static void release(Subject host) {
        if (host instanceof ItemSubject item) {
            item.getStack().remove(CabalistDataComponents.CONTRACT.get());
        } else if (host instanceof RuneSubject rune && rune.getLevel().getBlockEntity(rune.getPos()) instanceof ContractBlockEntity holder) {
            holder.releaseContract();
            if (rune.getLevel().getBlockState(rune.getPos()).is(CabalistBlocks.RUNE.get())) {
                rune.getLevel().removeBlock(rune.getPos(), false);
            }
        }
    }

    private boolean placeOnEntity(MinecraftServer server, Contract contract) {
        if (!(EntityHelper.find(server, entity) instanceof LivingEntity living)) {
            return false;
        }
        ItemStack held = living.getMainHandItem();
        if (held.isEmpty() || held.isStackable() || held.has(CabalistDataComponents.CONTRACT.get())) {
            return false;
        }
        held.set(CabalistDataComponents.CONTRACT.get(), contract.getUUID());
        return true;
    }

    private boolean placeOnBlock(MinecraftServer server, Contract contract) {
        ServerLevel level = dimension == null || pos == null ? null : server.getLevel(dimension);
        if (level == null) {
            return false;
        }
        if (level.getBlockEntity(pos) instanceof ContractBlockEntity holder && holder.getContract() == null) {
            holder.setContract(contract);
            return true;
        }
        ContractBlockEntity rune = RuneBlock.place(level, RuneBlock.getPlacePos(level, pos));
        if (rune == null) {
            return false;
        }
        rune.setContract(contract);
        return true;
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        if (entity != null) {
            tag.putUUID("entity", entity);
        }
        if (dimension != null && pos != null) {
            tag.putString("dimension", dimension.location().toString());
            tag.put("pos", NbtUtils.writeBlockPos(pos));
        }
        tag.putString("label", label);
        return tag;
    }

    public static HostTarget read(CompoundTag tag) {
        UUID entity = tag.hasUUID("entity") ? tag.getUUID("entity") : null;
        ResourceKey<Level> dimension = tag.contains("dimension") ? ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("dimension"))) : null;
        BlockPos pos = NbtUtils.readBlockPos(tag, "pos").orElse(null);
        return new HostTarget(entity, dimension, pos, tag.getString("label"));
    }
}
