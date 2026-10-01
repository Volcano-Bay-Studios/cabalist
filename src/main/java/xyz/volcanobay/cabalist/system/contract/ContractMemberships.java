package xyz.volcanobay.cabalist.system.contract;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The contracts an entity belongs to, saved on the entity.
 */
public class ContractMemberships {
    public static final Codec<ContractMemberships> CODEC = UUIDUtil.CODEC.listOf().xmap(ContractMemberships::new, ContractMemberships::getContractIds);

    private final List<UUID> contractIds;

    public ContractMemberships() {
        this.contractIds = new ArrayList<>();
    }

    private ContractMemberships(List<UUID> contractIds) {
        this.contractIds = new ArrayList<>(contractIds);
    }

    public List<UUID> getContractIds() {
        return contractIds;
    }

    public void add(UUID contractId) {
        if (!contractIds.contains(contractId)) {
            contractIds.add(contractId);
        }
    }

    public void remove(UUID contractId) {
        contractIds.remove(contractId);
    }
}
