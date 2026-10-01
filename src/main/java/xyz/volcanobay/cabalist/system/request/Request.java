package xyz.volcanobay.cabalist.system.request;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractState;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Requests are voted on by members of a contract.
 */
public class Request {
    private final UUID id;
    private final RequestKind kind;
    private final UUID contractId;
    private final UUID requester;
    private final UUID member;
    private final String payload;
    private final Map<UUID, PartyStatus> parties;
    private final Set<UUID> voters;
    private final Set<UUID> required;
    private final double threshold;
    private final @Nullable ContractState proposal;
    private final List<String> changes;
    private final Set<UUID> read = new HashSet<>();
    private final Set<UUID> dismissed = new HashSet<>();
    private @Nullable UUID worldAnsweredBy;
    private Outcome outcome = Outcome.PENDING;

    public Request(RequestKind kind, Contract contract, UUID requester, UUID member, String payload, Set<UUID> voters, Set<UUID> required,
                   double threshold, @Nullable ContractState proposal, List<String> changes) {
        this(UUID.randomUUID(), kind, contract.getUUID(), requester, member, payload, new LinkedHashMap<>(), voters, required, threshold, proposal, changes);
        for (UUID party : voters) {
            parties.put(party, PartyStatus.UNANSWERED);
        }
        for (UUID party : required) {
            parties.put(party, PartyStatus.UNANSWERED);
        }
        if (parties.containsKey(requester)) {
            parties.put(requester, PartyStatus.YES);
        }
    }

    private Request(UUID id, RequestKind kind, UUID contractId, UUID requester, UUID member, String payload, Map<UUID, PartyStatus> parties,
                    Set<UUID> voters, Set<UUID> required, double threshold, @Nullable ContractState proposal, List<String> changes) {
        this.id = id;
        this.kind = kind;
        this.contractId = contractId;
        this.requester = requester;
        this.member = member;
        this.payload = payload;
        this.parties = parties;
        this.voters = voters;
        this.required = required;
        this.threshold = threshold;
        this.proposal = proposal;
        this.changes = changes;
    }

    public @Nullable ContractState getProposal() {
        return proposal;
    }

    public List<String> getChanges() {
        return changes;
    }

    public Outcome tally() {
        int yes = 0;
        int no = 0;
        int unanswered = 0;
        for (UUID voter : voters) {
            PartyStatus status = getStatus(voter);
            if (status == PartyStatus.YES) {
                yes++;
            } else if (status.isRefusal()) {
                no++;
            } else {
                unanswered++;
            }
        }
        boolean isRequiredMet = true;
        for (UUID party : required) {
            PartyStatus status = getStatus(party);
            if (status.isRefusal()) {
                return Outcome.DENIED;
            }
            isRequiredMet &= status == PartyStatus.YES;
        }
        int needed = (int) Math.ceil(threshold * voters.size() - 1e-9);
        if (yes - no >= needed && isRequiredMet) {
            return Outcome.APPROVED;
        }
        return yes - no + unanswered < needed ? Outcome.DENIED : Outcome.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public RequestKind getKind() {
        return kind;
    }

    public @Nullable Contract getContract() {
        return ContractSystem.INSTANCE.getContract(contractId);
    }

    public UUID getContractId() {
        return contractId;
    }

    public UUID getRequester() {
        return requester;
    }

    public UUID getMember() {
        return member;
    }

    public String getPayload() {
        return payload;
    }

    public Map<UUID, PartyStatus> getParties() {
        return parties;
    }

    public PartyStatus getStatus(UUID party) {
        return parties.getOrDefault(party, PartyStatus.UNANSWERED);
    }

    public boolean needsAnswerFrom(UUID party) {
        return outcome == Outcome.PENDING && parties.get(party) == PartyStatus.UNANSWERED;
    }

    public boolean involves(UUID who) {
        return requester.equals(who) || parties.containsKey(who);
    }

    public boolean isRead(UUID who) {
        return read.contains(who);
    }

    public void markRead(UUID who) {
        read.add(who);
    }

    public boolean isDismissed(UUID who) {
        return dismissed.contains(who);
    }

    public void dismiss(UUID who) {
        dismissed.add(who);
    }

    public Set<UUID> getDismissed() {
        return dismissed;
    }

    public @Nullable UUID getWorldAnsweredBy() {
        return worldAnsweredBy;
    }

    public void setWorldAnsweredBy(@Nullable UUID admin) {
        worldAnsweredBy = admin;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    // The result is new to everyone, so it shows as unread again.
    public void decide(Outcome outcome) {
        this.outcome = outcome;
        read.clear();
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("kind", kind.name());
        tag.putUUID("contract", contractId);
        tag.putUUID("requester", requester);
        tag.putUUID("member", member);
        tag.putString("payload", payload);
        ListTag partyList = new ListTag();
        for (Map.Entry<UUID, PartyStatus> party : parties.entrySet()) {
            CompoundTag partyTag = new CompoundTag();
            partyTag.putUUID("id", party.getKey());
            partyTag.putString("status", party.getValue().name());
            partyList.add(partyTag);
        }
        tag.put("parties", partyList);
        tag.put("voters", writeIds(voters));
        tag.put("required", writeIds(required));
        tag.putDouble("threshold", threshold);
        if (proposal != null) {
            tag.put("proposal", proposal.write());
        }
        ListTag changeList = new ListTag();
        for (String change : changes) {
            changeList.add(StringTag.valueOf(change));
        }
        tag.put("changes", changeList);
        tag.put("read", writeIds(read));
        tag.put("dismissed", writeIds(dismissed));
        if (worldAnsweredBy != null) {
            tag.putUUID("world_answered_by", worldAnsweredBy);
        }
        tag.putString("outcome", outcome.name());
        return tag;
    }

    // Returns null for requests saved in the old format, which are dropped.
    public static @Nullable Request read(CompoundTag tag) {
        if (!tag.hasUUID("id") || !tag.contains("voters")) {
            return null;
        }
        Map<UUID, PartyStatus> parties = new LinkedHashMap<>();
        for (Tag entry : tag.getList("parties", Tag.TAG_COMPOUND)) {
            CompoundTag partyTag = (CompoundTag) entry;
            parties.put(partyTag.getUUID("id"), PartyStatus.valueOf(partyTag.getString("status")));
        }
        Set<UUID> voters = new HashSet<>();
        readIds(tag.getList("voters", Tag.TAG_INT_ARRAY), voters);
        Set<UUID> required = new HashSet<>();
        readIds(tag.getList("required", Tag.TAG_INT_ARRAY), required);
        ContractState proposal = tag.contains("proposal") ? ContractState.read(tag.getCompound("proposal")) : null;
        List<String> changes = new ArrayList<>();
        ListTag changeList = tag.getList("changes", Tag.TAG_STRING);
        for (int i = 0; i < changeList.size(); i++) {
            changes.add(changeList.getString(i));
        }
        Request request = new Request(tag.getUUID("id"), RequestKind.valueOf(tag.getString("kind")), tag.getUUID("contract"),
                tag.getUUID("requester"), tag.getUUID("member"), tag.getString("payload"), parties, voters, required,
                tag.getDouble("threshold"), proposal, List.copyOf(changes));
        readIds(tag.getList("read", Tag.TAG_INT_ARRAY), request.read);
        readIds(tag.getList("dismissed", Tag.TAG_INT_ARRAY), request.dismissed);
        request.worldAnsweredBy = tag.hasUUID("world_answered_by") ? tag.getUUID("world_answered_by") : null;
        request.outcome = Outcome.valueOf(tag.getString("outcome"));
        return request;
    }

    private static ListTag writeIds(Set<UUID> ids) {
        ListTag list = new ListTag();
        for (UUID id : ids) {
            list.add(NbtUtils.createUUID(id));
        }
        return list;
    }

    private static void readIds(ListTag list, Set<UUID> out) {
        for (Tag id : list) {
            out.add(NbtUtils.loadUUID(id));
        }
    }

    public enum Outcome {
        PENDING,
        APPROVED,
        DENIED
    }
}
