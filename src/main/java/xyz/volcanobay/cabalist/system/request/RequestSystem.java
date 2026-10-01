package xyz.volcanobay.cabalist.system.request;

import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.core.CabalistRequestStyles;
import xyz.volcanobay.cabalist.networking.packet.RequestsS2CPacket;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractState;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.contract.ContractTerms;
import xyz.volcanobay.cabalist.system.contract.WorldContractee;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Requests cannot expire. Players that are offline still must answer.
 * Anyone can see another's circles.
 */
public class RequestSystem {
    public static final RequestSystem INSTANCE = new RequestSystem();
    private static final int RESEND_INTERVAL_TICKS = 100;

    private final List<Request> requests = new ArrayList<>();
    private final Map<UUID, List<RequestsS2CPacket.Entry>> synced = new HashMap<>();
    private final Map<UUID, Set<UUID>> openBy = new HashMap<>();
    private boolean isChanged = true;

    public List<Request> getRequests() {
        return requests;
    }

    public @Nullable Request get(UUID id) {
        for (Request request : requests) {
            if (request.getId().equals(id)) {
                return request;
            }
        }
        return null;
    }

    public void submitMembership(Contract contract, RequestKind kind, UUID requester, UUID member) {
        submitMembership(contract, kind, requester, member, false);
    }

    public void submitMembership(Contract contract, RequestKind kind, UUID requester, UUID member, boolean memberConsented) {
        Contract joiningContract = ContractSystem.INSTANCE.getContract(member);
        if (kind == RequestKind.JOIN && (joiningContract == contract || joiningContract != null && contract.isWithin(joiningContract))) {
            return;
        }
        if (kind == RequestKind.JOIN && isOverLimit(contract, member)) {
            contract.logAction(requester, "join", member.toString(), "denied by a limit");
            return;
        }
        Set<UUID> required = new LinkedHashSet<>();
        required.add(requester);
        required.add(joiningContract != null && joiningContract.getCreatorId() != null ? joiningContract.getCreatorId() : member);
        if (kind == RequestKind.JOIN && contract.getCreatorId() != null) {
            required.add(contract.getCreatorId());
        }
        Request request = new Request(kind, contract, requester, member, "", Set.of(), required, 0, null, List.of());
        if (kind == RequestKind.JOIN && (member.equals(WorldContractee.WORLD_ID) || joiningContract != null && joiningContract.getCreatorId() == null)) {
            request.getParties().put(member, PartyStatus.YES);
        }
        if (memberConsented && joiningContract == null) {
            request.getParties().put(member, PartyStatus.YES);
        }
        add(request, contract);
    }

    public boolean submitAmend(Contract contract, UUID requester, ContractState proposal) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || AmendCooldownSystem.INSTANCE.isWaiting(requester, contract.getUUID(), server.overworld().getGameTime())) {
            return false;
        }
        List<String> changes = proposal.describeChanges(contract);
        if (changes.isEmpty()) {
            return false;
        }
        ContractTerms.AmendRules rules = ContractTerms.read(contract, server);
        Set<UUID> required = new LinkedHashSet<>(rules.required());
        if (proposal.rebind() != null && proposal.rebind().getConsentingParty() != null) {
            required.add(proposal.rebind().getConsentingParty());
        }
        Contract authority = contract.getNetworkAuthority();
        if (authority != null && authority != contract && ContractTerms.isAuthorizing(proposal.getTexts())) {
            return false;
        }
        add(new Request(RequestKind.AMEND, contract, requester, requester, "", rules.getVoters(contract), required, rules.threshold(), proposal, changes), contract);
        return true;
    }

    public boolean destroyOrPropose(Contract contract, Subject caster) {
        UUID id = caster.getUUID();
        if (contract.isArbiter(caster)) {
            contract.destroy(id);
            return true;
        }
        return id != null && contract.hasMember(id) && submitAmend(contract, id, ContractState.of(contract).destroyed());
    }

    /**
     * Limits prevent people from joining contracts if another doesn't allow them to.
     */
    private static boolean isOverLimit(Contract joining, UUID member) {

        for (String tag : joining.getSettings().tags()) {
            int already = 0;
            int limit = Integer.MAX_VALUE;
            for (Contract other : ContractSystem.INSTANCE.getContracts()) {
                if (!other.getSettings().tags().contains(tag)) {
                    continue;
                }
                if (other != joining && other.hasMember(member)) {
                    already++;
                }
                for (ContractTerms.Limit set : other.getSettings().limits()) {
                    if (set.tag().equals(tag)) {
                        limit = Math.min(limit, set.count());
                    }
                }
            }
            if (already + 1 > limit) {
                return true;
            }
        }
        return false;
    }

    private void add(Request request, Contract contract) {
        contract.logAction(request.getRequester(), getActionName(request), String.join("; ", request.getChanges()), "requested");
        requests.add(request);
        isChanged = true;
        Request.Outcome outcome = request.tally();
        if (outcome != Request.Outcome.PENDING) {
            decide(request, outcome);
        }
    }

    public boolean answer(Request request, UUID who, PartyStatus status, @Nullable UUID admin) {
        if (!request.needsAnswerFrom(who) || status == PartyStatus.UNANSWERED) {
            return false;
        }
        request.getParties().put(who, status);
        if (who.equals(WorldContractee.WORLD_ID)) {
            request.setWorldAnsweredBy(admin);
        }
        Contract contract = request.getContract();
        if (contract != null) {
            contract.logAction(admin != null ? admin : who, "answer", getActionName(request), status.name().toLowerCase(Locale.ROOT));
        }
        Request.Outcome outcome = request.tally();
        if (outcome != Request.Outcome.PENDING) {
            decide(request, outcome);
        }
        isChanged = true;
        return true;
    }

    public int answerContract(Contract contract, UUID who, PartyStatus status, @Nullable UUID admin) {
        int answered = 0;
        for (Request request : List.copyOf(requests)) {
            if (request.getContractId().equals(contract.getUUID()) && answer(request, who, status, admin)) {
                answered++;
            }
        }
        return answered;
    }

    public void markRead(Request request, UUID who) {
        if (!request.isRead(who)) {
            request.markRead(who);
            isChanged = true;
        }
    }

    public void dismiss(Request request, UUID who) {
        if (request.getOutcome() == Request.Outcome.PENDING || request.isDismissed(who)) {
            return;
        }
        request.dismiss(who);
        boolean isDone = request.getDismissed().contains(request.getRequester());
        for (UUID party : request.getParties().keySet()) {
            isDone &= party.equals(WorldContractee.WORLD_ID) || request.isDismissed(party);
        }
        if (isDone) {
            requests.remove(request);
        }
        isChanged = true;
    }

    private void decide(Request request, Request.Outcome outcome) {
        request.decide(outcome);
        Contract contract = request.getContract();
        if (contract == null) {
            return;
        }
        if (outcome == Request.Outcome.APPROVED) {
            apply(request, contract);
            return;
        }
        contract.logAction(request.getRequester(), getActionName(request), String.join("; ", request.getChanges()), "denied");
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (request.getKind() == RequestKind.AMEND && server != null) {
            AmendCooldownSystem.INSTANCE.start(request.getRequester(), contract.getUUID(), server.overworld().getGameTime());
        }
    }

    private static void apply(Request request, Contract contract) {
        switch (request.getKind()) {
            case JOIN -> {
                if (isOverLimit(contract, request.getMember())) {
                    contract.logAction(request.getRequester(), "join", request.getMember().toString(), "denied by a limit");
                    return;
                }
                contract.addMemberId(request.getMember());
                contract.logAction(request.getRequester(), "join", request.getMember().toString(), "accepted");
                boolean isPerson = !request.getMember().equals(WorldContractee.WORLD_ID) && ContractSystem.INSTANCE.getContract(request.getMember()) == null;
                if (contract.getCreatorId() == null && isPerson) {
                    contract.setCreatorId(request.getMember());
                    contract.logAction(request.getMember(), "arbiter", request.getMember().toString(), "accepted");
                }
            }
            case LEAVE -> {
                contract.removeMemberId(request.getMember());
                contract.logAction(request.getRequester(), "leave", request.getMember().toString(), "accepted");
            }
            case AMEND -> {
                if (request.getProposal() != null) {
                    contract.applyState(request.getProposal(), request.getRequester());
                }
            }
        }
    }

    private static String getActionName(Request request) {
        return request.getKind().name().toLowerCase(Locale.ROOT);
    }

    public void markChanged() {
        isChanged = true;
    }

    public void removeFor(Contract contract) {
        if (requests.removeIf(request -> request.getContractId().equals(contract.getUUID()))) {
            isChanged = true;
        }
    }

    public void tick(MinecraftServer server) {
        boolean isResend = server.getTickCount() % RESEND_INTERVAL_TICKS == 0;
        AmendCooldownSystem.INSTANCE.prune(server.overworld().getGameTime());
        if (!isChanged && !isResend) {
            return;
        }
        isChanged = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            List<RequestsS2CPacket.Entry> entries = collect(player);
            boolean isEntriesChanged = !entries.equals(synced.get(player.getUUID()));
            if (isEntriesChanged || isResend && !entries.isEmpty()) {
                synced.put(player.getUUID(), entries);
                RequestsS2CPacket packet = new RequestsS2CPacket(player.getId(), entries);
                if (player.isSpectator()) {
                    VeilPacketManager.player(player).sendPacket(packet);
                } else {
                    VeilPacketManager.trackingAndSelf(player).sendPacket(packet);
                }
            }
        }
        synced.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        openBy.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    public void setOpen(ServerPlayer player, UUID request, boolean isOpen) {
        Set<UUID> open = openBy.computeIfAbsent(player.getUUID(), id -> new HashSet<>());
        if (isOpen) {
            open.add(request);
        } else {
            open.remove(request);
        }
        markChanged();
    }

    public void onLogin(ServerPlayer player) {
        synced.remove(player.getUUID());
        markChanged();
    }

    private List<RequestsS2CPacket.Entry> collect(ServerPlayer player) {
        UUID self = player.getUUID();
        boolean isWorldMode = WorldConsent.isActive(player);
        List<RequestsS2CPacket.Entry> entries = new ArrayList<>();
        for (Request request : requests) {
            boolean isPending = request.getOutcome() == Request.Outcome.PENDING;
            boolean hasWorld = request.getParties().containsKey(WorldContractee.WORLD_ID);
            if (isPending) {
                if (request.needsAnswerFrom(self)) {
                    entries.add(toEntry(request, self, RequestsS2CPacket.ROLE_ANSWER, false));
                } else if (isWorldMode && request.needsAnswerFrom(WorldContractee.WORLD_ID)) {
                    entries.add(toEntry(request, self, RequestsS2CPacket.ROLE_ANSWER, true));
                }
                if (request.getRequester().equals(self)) {
                    entries.add(toEntry(request, self, RequestsS2CPacket.ROLE_STATUS, false));
                }
            } else if (!request.isDismissed(self) && (request.involves(self) || isWorldMode && hasWorld)) {
                entries.add(toEntry(request, self, RequestsS2CPacket.ROLE_RESULT, !request.involves(self)));
            }
        }
        for (NoticeSystem.Notice notice : NoticeSystem.INSTANCE.getAll()) {
            if (notice.owner().equals(self)) {
                Vec3 about = notice.about();
                List<String> args = List.of(notice.title(), Double.toString(about.x), Double.toString(about.y), Double.toString(about.z));
                entries.add(new RequestsS2CPacket.Entry(notice.id(), RequestsS2CPacket.ROLE_INFO, "request.cabalist.appraisal", args,
                        notice.color(), false, false, 0, List.of(), false, notice.lines()));
            }
        }
        AmendCooldownSystem.INSTANCE.getAll().forEach((key, until) -> {
            if (key.author().equals(self)) {
                entries.add(toCooldownEntry(key, until));
            }
        });
        return entries;
    }

    private static RequestsS2CPacket.Entry toCooldownEntry(AmendCooldownSystem.Key key, long until) {
        UUID id = UUID.nameUUIDFromBytes((key.author() + "/" + key.contract()).getBytes(StandardCharsets.UTF_8));
        return new RequestsS2CPacket.Entry(id, RequestsS2CPacket.ROLE_COOLDOWN, "request.cabalist.cooldown",
                List.of(ContractSystem.INSTANCE.getContracteeName(key.contract()), Long.toString(until)), CabalistRequestStyles.get("cooldown").color(),
                false, false, 0, List.of(), false, List.of());
    }

    private RequestsS2CPacket.Entry toEntry(Request request, UUID viewer, int role, boolean isWorld) {
        ContractSystem contracts = ContractSystem.INSTANCE;
        boolean isForSelf = request.getMember().equals(request.getRequester());
        List<String> args = List.of(contracts.getContracteeName(request.getRequester()), contracts.getContracteeName(request.getContractId()),
                contracts.getContracteeName(request.getMember()), request.getPayload());
        List<RequestsS2CPacket.Party> parties = new ArrayList<>();
        for (Map.Entry<UUID, PartyStatus> party : request.getParties().entrySet()) {
            PartyStatus status = party.getValue();
            parties.add(new RequestsS2CPacket.Party(contracts.getContracteeName(party.getKey()), status.ordinal(), CabalistRequestStyles.get(status.getStyleName()).color()));
        }
        int color = CabalistRequestStyles.get(request.getKind().getStyleName()).getColor(isWorld);
        return new RequestsS2CPacket.Entry(request.getId(), role, request.getKind().getTranslationKey(isForSelf), args, color, isWorld,
                request.isRead(viewer), request.getOutcome().ordinal(), parties, openBy.getOrDefault(viewer, Set.of()).contains(request.getId()), request.getChanges());
    }

    public void clear() {
        requests.clear();
        synced.clear();
        openBy.clear();
        isChanged = true;
    }

    public ListTag write() {
        ListTag list = new ListTag();
        for (Request request : requests) {
            list.add(request.write());
        }
        return list;
    }

    public void read(ListTag list) {
        requests.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            Request request = Request.read(tag);
            if (request != null) {
                requests.add(request);
            }
        }
        isChanged = true;
    }
}
