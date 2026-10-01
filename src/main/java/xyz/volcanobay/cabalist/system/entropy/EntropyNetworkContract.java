package xyz.volcanobay.cabalist.system.entropy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.core.CabalistSpatialNetworks;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.energy.EnergyStack;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.system.subject.SubjectList;
import xyz.volcanobay.cabalist.util.BlockHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EntropyNetworkContract extends Contract {
    public static final String TYPE = "entropy_network";
    private static final Vector3d POSITION = new Vector3d();
    private static final BlockPos.MutableBlockPos TOUCHING = new BlockPos.MutableBlockPos();

    private final NetworkEnergyStack networkStack = new NetworkEnergyStack(this);
    private ResourceKey<Level> dimension = Level.OVERWORLD;
    private int networkId;
    private @Nullable Level level;

    public EntropyNetworkContract() {
        super("Entropy network");
    }

    public static Contract getOrCreate(Level level, int networkId) {
        Contract existing = ContractSystem.INSTANCE.getNetworkContract(level.dimension(), networkId);
        if (existing != null) {
            return existing;
        }
        EntropyNetworkContract contract = new EntropyNetworkContract();
        contract.dimension = level.dimension();
        contract.networkId = networkId;
        contract.level = level;
        ContractSystem.INSTANCE.addContract(contract);
        return contract;
    }

    public static @Nullable EntropyNetwork findTouchingNetwork(Subject subject) {
        List<EntropyNetwork> touching = getTouchingNetworks(subject);
        return touching.isEmpty() ? null : touching.get(0);
    }

    public static void collectTouching(Subject subject, SubjectList out) {
        Level level = subject.getLevel();
        for (EntropyNetwork network : getTouchingNetworks(subject)) {
            Contract contract = ContractSystem.INSTANCE.getNetworkContract(level.dimension(), network.getId());
            if (contract != null && !out.contains(contract)) {
                out.add(contract);
            }
        }
    }

    // Networks in the blocks around and under the subject, nearest corner first.
    private static List<EntropyNetwork> getTouchingNetworks(Subject subject) {
        List<EntropyNetwork> networks = new ArrayList<>();
        Level level = subject.getLevel();
        if (level == null) {
            return networks;
        }
        subject.getPosition(POSITION);
        int baseX = (int) Math.floor(POSITION.x);
        int baseY = (int) Math.floor(POSITION.y);
        int baseZ = (int) Math.floor(POSITION.z);
        for (int dy = -1; dy <= 0; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    TOUCHING.set(baseX + dx, baseY + dy, baseZ + dz);
                    if (BlockHelper.isEntropetic(level.getBlockState(TOUCHING), TOUCHING, level)) {
                        EntropyNetwork network = CabalistSpatialNetworks.ENTROPY_NETWORK.get(level).getNetwork(TOUCHING);
                        if (network != null && !networks.contains(network)) {
                            networks.add(network);
                        }
                    }
                }
            }
        }
        return networks;
    }

    public void bind(Level level) {
        if (level.dimension().equals(dimension)) {
            this.level = level;
        }
    }

    public @Nullable EntropyNetwork getNetwork() {
        if (level == null) {
            return null;
        }
        return CabalistSpatialNetworks.ENTROPY_NETWORK.get(level).getNetwork(networkId);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean isForNetwork(ResourceKey<Level> dimension, int networkId) {
        return this.dimension.equals(dimension) && this.networkId == networkId;
    }

    @Override
    public void tick(MinecraftServer server) {
        if (level == null) {
            level = server.getLevel(dimension);
        }
        super.tick(server);
    }

    @Override
    public void collectMembers(SubjectList out) {
        out.add(this);
    }

    @Override
    public boolean consents(Subject asker, Spell spell) {
        UUID askerId = asker.getUUID();
        return askerId != null && hasMember(askerId);
    }

    @Override
    public EnergyStack getEnergyStack() {
        return networkStack;
    }

    @Override
    public @Nullable Level getLevel() {
        return level;
    }

    public Component getDisplayName() {
        Contract authority = getAuthority();
        if (authority != null && !authority.getName().isEmpty()) {
            return Component.literal(authority.getName());
        }
        return Component.literal("Entropy network " + networkId);
    }

    public @Nullable Contract getAuthority() {
        return ContractSystem.INSTANCE.getNetworkAuthority(level, networkId);
    }

    @Override
    public CompoundTag write(CompoundTag tag) {
        super.write(tag);
        tag.putString("dimension", dimension.location().toString());
        tag.putInt("network", networkId);
        return tag;
    }

    @Override
    public void read(CompoundTag tag) {
        super.read(tag);
        ResourceLocation location = ResourceLocation.tryParse(tag.getString("dimension"));
        if (location != null) {
            dimension = ResourceKey.create(Registries.DIMENSION, location);
        }
        networkId = tag.getInt("network");
    }
}
