package xyz.volcanobay.cabalist.system.contract;

import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The world itself is a contract. It accepts every invitation but never consents to any request,
 * so a contract that includes it can no longer be changed.
 */
public class WorldContractee implements Contractee {
    public static final UUID WORLD_ID = UUID.nameUUIDFromBytes("cabalist:world".getBytes(StandardCharsets.UTF_8));
    public static final WorldContractee INSTANCE = new WorldContractee();

    private final List<Contract> activeContracts = new ArrayList<>();

    private WorldContractee() {
    }

    @Override
    public UUID getUUID() {
        return WORLD_ID;
    }

    @Override
    public void tickContract(Contract contract) {
    }

    @Override
    public List<Contract> getActiveContractsList() {
        return activeContracts;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("The World");
    }
}
