package xyz.volcanobay.cabalist.system.contract;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.blockentity.ContractBlockEntity;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.core.CabalistDataComponents;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.energy.EnergyStack;
import xyz.volcanobay.cabalist.system.energy.MembersEnergyStack;
import xyz.volcanobay.cabalist.system.entropy.EntropyNetwork;
import xyz.volcanobay.cabalist.system.entropy.EntropyNetworkContract;
import xyz.volcanobay.cabalist.system.focus.Focus;
import xyz.volcanobay.cabalist.system.render.Recoloring;
import xyz.volcanobay.cabalist.system.spell.PendingSpell;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.spell.SpellEngine;
import xyz.volcanobay.cabalist.system.spell.TriggerEvent;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.ItemSubject;
import xyz.volcanobay.cabalist.system.subject.RuneSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.system.subject.SubjectList;
import xyz.volcanobay.cabalist.util.EntityHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Contract extends Subject implements Contractee {
    private static final int MAX_LOG_SIZE = 256;
    private static final int NETWORK_CHECK_TICKS = 20;

    protected final EnergyStack stack = new EnergyStack();
    private final List<UUID> memberIds = new ArrayList<>();
    private final List<Contractee> contractees = new ArrayList<>();
    private final TermSet terms = new TermSet();
    private final ArrayList<Contract> activeContracts = new ArrayList<>();
    private final List<String> incantations = new ArrayList<>();
    private final Map<String, List<Integer>> spellColors = new HashMap<>();
    private final List<PendingSpell> spells = new ArrayList<>();
    private final List<ContractAction> log = new ArrayList<>();
    private UUID uuid = UUID.randomUUID();
    private String name = "";
    private @Nullable UUID creatorId;
    private @Nullable Subject host;
    private double imbued;
    private int networkCheck;
    private int networkId = -1;
    private int energySource;
    private @Nullable MembersEnergyStack membersStack;
    private ContractTerms.@Nullable Settings settings;
    private int settingsHash;

    public Contract() {
    }

    public Contract(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getShownName() {
        return name.isEmpty() ? "an unnamed contract" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public @Nullable UUID getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(@Nullable UUID creatorId) {
        this.creatorId = creatorId;
    }

    public List<String> getIncantations() {
        return incantations;
    }

    public List<ContractAction> getLog() {
        return log;
    }

    public void logAction(@Nullable UUID actor, String action, String detail, String outcome) {
        log.add(new ContractAction(System.currentTimeMillis(), actor, action, detail, outcome));
        if (log.size() > MAX_LOG_SIZE) {
            log.remove(0);
        }
    }

    public void addSpell(String incantation, @Nullable UUID actor) {
        incantations.add(incantation);
        if (host != null && !ContractTerms.isTerm(incantation)) {
            spells.add(createPendingSpell(incantation, host));
        }
        logAction(actor, "append", incantation, "accepted");
    }

    public void applyState(ContractState state, @Nullable UUID actor) {
        if (state.isDestroyed()) {
            destroy(actor);
            return;
        }
        name = state.name();
        incantations.clear();
        incantations.addAll(state.getTexts());
        if (host != null) {
            List<PendingSpell> previous = new ArrayList<>(spells);
            spells.clear();
            for (String incantation : incantations) {
                if (ContractTerms.isTerm(incantation)) {
                    continue;
                }
                PendingSpell kept = previous.stream().filter(spell -> spell.getIncantation().equals(incantation)).findFirst().orElse(null);
                if (kept != null) {
                    previous.remove(kept);
                    spells.add(kept);
                } else {
                    spells.add(createPendingSpell(incantation, host));
                }
            }
            previous.forEach(PendingSpell::dismiss);
        }
        logAction(actor, "amend", String.join("; ", incantations), "accepted");
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (state.rebind() != null && server != null) {
            boolean isMoved = state.rebind().move(this, server);
            logAction(actor, "rebind", state.rebind().describe(), isMoved ? "accepted" : "failed");
        }
    }

    public List<PendingSpell> getPendingSpells() {
        return spells;
    }

    public @Nullable Subject getHost() {
        return host;
    }

    public void bindHost(Subject newHost) {
        if (newHost.equals(host)) {
            return;
        }
        host = newHost;
        networkCheck = 0;
        rebuildSpells();
    }

    private void rebuildSpells() {
        spells.forEach(PendingSpell::dismiss);
        spells.clear();
        if (host == null) {
            return;
        }
        for (String incantation : incantations) {
            if (!ContractTerms.isTerm(incantation)) {
                spells.add(createPendingSpell(incantation, host));
            }
        }
    }

    public void unbindHost(Subject oldHost) {
        if (oldHost.equals(host)) {
            host = null;
            spells.forEach(PendingSpell::release);
            spells.clear();
        }
    }

    private PendingSpell createPendingSpell(String incantation, Subject spellHost) {
        Spell spell = SpellEngine.INSTANCE.resolve(incantation, spellHost, 0);
        spell.setIncantation(incantation);
        spell.setName(name);
        spell.setColors(spellColors.getOrDefault(incantation, List.of()));
        return new PendingSpell(spell, spellHost, true, getSpellStack());
    }

    public void recolor(String incantation, List<Integer> colors) {
        spellColors.put(incantation, List.copyOf(colors));
        for (PendingSpell pending : spells) {
            if (pending.getIncantation().equals(incantation)) {
                Recoloring.apply(pending, colors);
            }
        }
    }

    public void recolorAll(List<Integer> colors) {
        for (String incantation : incantations) {
            if (!ContractTerms.isTerm(incantation)) {
                recolor(incantation, colors);
            }
        }
    }

    public ContractTerms.Settings getSettings() {
        int hash = incantations.hashCode();
        if (settings == null || hash != settingsHash) {
            settings = ContractTerms.readSettings(this);
            settingsHash = hash;
        }
        return settings;
    }

    private EnergyStack getSpellStack() {
        if (getSettings().isMembersPay()) {
            if (membersStack == null) {
                membersStack = new MembersEnergyStack(this::getPayers);
            }
            return membersStack;
        }
        Contract network = getNetworkContract();
        return network == null ? stack : network.getEnergyStack();
    }

    public @Nullable Contract getNetworkContract() {
        Level level = getLevel();
        return networkId < 0 || level == null ? null : ContractSystem.INSTANCE.getNetworkContract(level.dimension(), networkId);
    }

    private Set<UUID> getPayers() {
        Set<UUID> payers = getCastingMemberIds(getSettings().isMembersPayEverywhere());
        Contract network = getNetworkContract();
        if (network != null) {
            payers.addAll(network.getMemberIds());
        }
        return payers;
    }

    public Set<UUID> getCastingMemberIds(boolean isEverywhere) {
        Set<UUID> ids = new HashSet<>();
        for (UUID id : memberIds) {
            if (ContractSystem.INSTANCE.getContract(id) == null) {
                ids.add(id);
            }
        }
        ids.addAll(getParentMemberIds(new HashSet<>()));
        if (!isEverywhere) {
            ids.removeIf(id -> !isInRange(id));
        }
        ids.addAll(getSharedMemberIds());
        return ids;
    }

    public Set<UUID> getCastingMemberIds() {
        return getCastingMemberIds(false);
    }

    public Set<UUID> getSharedMemberIds() {
        Set<UUID> shared = new HashSet<>();
        Contract authority = getNetworkAuthority();
        if (authority == null || authority == this) {
            return shared;
        }
        shared.addAll(authority.memberIds);
        shared.removeIf(id -> ContractSystem.INSTANCE.getContract(id) != null);
        if (!authority.getSettings().isAuthorizingEverywhere()) {
            shared.removeIf(id -> !isInRange(id));
        }
        return shared;
    }

    public @Nullable Contract getNetworkAuthority() {
        return ContractSystem.INSTANCE.getNetworkAuthority(getLevel(), networkId);
    }

    public List<Contract> getParentContracts() {
        List<Contract> parents = new ArrayList<>();
        for (Contract other : ContractSystem.INSTANCE.getContracts()) {
            if (other != this && other.memberIds.contains(uuid)) {
                parents.add(other);
            }
        }
        return parents;
    }

    private Set<UUID> getParentMemberIds(Set<Contract> visited) {
        Set<UUID> ids = new HashSet<>();
        if (!visited.add(this)) {
            return ids;
        }
        for (Contract parent : getParentContracts()) {
            for (UUID id : parent.memberIds) {
                if (ContractSystem.INSTANCE.getContract(id) == null) {
                    ids.add(id);
                }
            }
            if (parent.creatorId != null) {
                ids.add(parent.creatorId);
            }
            ids.addAll(parent.getParentMemberIds(visited));
        }
        return ids;
    }

    public boolean isWithin(Contract other) {
        return isWithin(other, new HashSet<>());
    }

    private boolean isWithin(Contract other, Set<Contract> visited) {
        if (!visited.add(this)) {
            return false;
        }
        for (Contract parent : getParentContracts()) {
            if (parent == other || parent.isWithin(other, visited)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInRange(UUID id) {
        double radius = getSettings().radius();
        if (Double.isNaN(radius) || host == null || id.equals(WorldContractee.WORLD_ID)) {
            return true;
        }
        Level level = host.getLevel();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (level == null || server == null) {
            return true;
        }
        Entity entity = server.getPlayerList().getPlayer(id);
        if (entity == null && level instanceof ServerLevel serverLevel) {
            entity = serverLevel.getEntity(id);
        }
        if (entity == null || entity.level() != level) {
            return false;
        }
        Vector3d center = new Vector3d();
        host.getPosition(center);
        return entity.position().distanceToSqr(center.x, center.y, center.z) <= radius * radius;
    }

    public int getNetworkId() {
        return networkId;
    }

    private void refreshNetwork() {
        if (--networkCheck > 0) {
            return;
        }
        networkCheck = NETWORK_CHECK_TICKS;
        EntropyNetwork network = host instanceof RuneSubject ? EntropyNetworkContract.findTouchingNetwork(host) : null;
        int found = network == null ? -1 : network.getId();
        Level level = getLevel();
        if (found >= 0 && level != null) {
            EntropyNetworkContract.getOrCreate(level, found);
        }
        int source = getSettings().isMembersPay() ? 2 : found < 0 ? 0 : 1;
        if (found != networkId || source != energySource) {
            networkId = found;
            energySource = source;
            rebuildSpells();
        }
    }

    public void onTrigger(TriggerEvent event, long gameTime) {
        for (PendingSpell spell : spells) {
            spell.onTrigger(event, gameTime);
        }
    }

    @Override
    public void collectMembers(SubjectList out) {
        if (memberIds.isEmpty()) {
            out.add(this);
            return;
        }
        for (Contractee contractee : contractees) {
            if (!(contractee instanceof Contract)) {
                contractee.collectMembers(out);
            }
        }
    }

    @Override
    public @Nullable Level getLevel() {
        return host == null ? null : host.getLevel();
    }

    @Override
    public void getPosition(Vector3d out) {
        if (host == null) {
            out.zero();
            return;
        }
        host.getPosition(out);
    }

    @Override
    public void getFacing(Vector3d out) {
        if (host == null) {
            out.zero();
            return;
        }
        host.getFacing(out);
    }

    @Override
    public boolean receive(Aspect aspect, SpellClause clause, float magnitude) {
        return aspect.affectContract(this, clause, magnitude);
    }

    @Override
    public EnergyStack getEnergyStack() {
        return stack;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(name.isEmpty() ? "Unnamed contract" : name);
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public void tickContract(Contract contract) {
    }

    public List<UUID> getMemberIds() {
        return memberIds;
    }

    public List<Contractee> getLoadedMembers() {
        return contractees;
    }

    public boolean hasMember(UUID memberId) {
        return memberIds.contains(memberId);
    }

    public boolean isContractee(UUID id) {
        return hasMember(id) || id.equals(creatorId);
    }

    public boolean canBeNamedBy(Subject caster) {
        UUID id = caster.getUUID();
        if (id != null && (isContractee(id) || getParentMemberIds(new HashSet<>()).contains(id))) {
            return true;
        }
        Contract bound = caster.getBoundContract();
        return bound != null && canBeNamedBy(bound);
    }

    public boolean canBeNamedBy(Contract other) {
        return other == this || hasMember(other.uuid) || other.hasMember(uuid) || other.creatorId != null && isContractee(other.creatorId);
    }

    public Set<UUID> getContracteeIds() {
        Set<UUID> ids = new HashSet<>(memberIds);
        if (creatorId != null) {
            ids.add(creatorId);
        }
        return ids;
    }

    public void addMember(Contractee contractee) {
        if (contractee == this || hasMember(contractee.getUUID())) {
            return;
        }
        memberIds.add(contractee.getUUID());
        contractees.add(contractee);
        contractee.joinContract(this);
    }

    public void addMemberId(UUID memberId) {
        if (!memberId.equals(uuid) && !hasMember(memberId)) {
            memberIds.add(memberId);
        }
    }

    public void removeMember(Contractee contractee) {
        removeMemberId(contractee.getUUID());
        contractee.leaveContract(this);
    }

    public void removeMemberId(UUID memberId) {
        memberIds.remove(memberId);
        if (memberId.equals(creatorId)) {
            passArbiter();
        }
        for (int i = contractees.size() - 1; i >= 0; i--) {
            Contractee contractee = contractees.get(i);
            if (contractee.getUUID().equals(memberId)) {
                contractees.remove(i);
                contractee.leaveContract(this);
            }
        }
    }

    public void destroy(@Nullable UUID actor) {
        if (host instanceof RuneSubject rune && rune.getLevel().getBlockEntity(rune.getPos()) instanceof ContractBlockEntity holder) {
            holder.releaseContract();
        } else if (host instanceof ItemSubject item) {
            item.getStack().remove(CabalistDataComponents.CONTRACT.get());
        }
        logAction(actor, "destroy", "", "accepted");
        ContractSystem.INSTANCE.removeContract(this);
    }

    public boolean isArbiter(Subject caster) {
        return isArbiter(caster, new HashSet<>());
    }

    private boolean isArbiter(Subject caster, Set<Contract> visited) {
        UUID id = caster.getUUID();
        if (id != null && id.equals(creatorId) || caster.getBoundContract() == this) {
            return true;
        }
        if (!visited.add(this)) {
            return false;
        }
        for (Contract parent : getParentContracts()) {
            if (parent.isArbiter(caster, visited)) {
                return true;
            }
        }
        return false;
    }

    private void passArbiter() {
        creatorId = null;
        for (UUID member : memberIds) {
            if (!member.equals(WorldContractee.WORLD_ID) && ContractSystem.INSTANCE.getContract(member) == null) {
                creatorId = member;
                break;
            }
        }
        logAction(creatorId, "arbiter", creatorId == null ? "none" : creatorId.toString(), "accepted");
    }

    public String getType() {
        return "contract";
    }

    public boolean isForNetwork(ResourceKey<Level> dimension, int networkId) {
        return false;
    }

    public void addTerm(Term term) {
        terms.add(term);
    }

    public double getImbued() {
        return imbued;
    }

    public void imbue(double amount) {
        imbued += Math.max(0, amount);
    }

    private void recharge() {
        if (!(host instanceof ItemSubject item)) {
            return;
        }
        double cap = imbued + Focus.get(item.getStack()).getTotalLifeforce();
        double missing = cap - stack.get(CabalistEnergyTypes.ENTROPY.get());
        if (missing > 0) {
            stack.give(CabalistEnergyTypes.ENTROPY.get(), Math.min(missing, cap / (CabalistConfig.CONTRACT_RECHARGE_SECONDS.get() * 20)));
        }
    }

    public void tick(MinecraftServer server) {
        recharge();
        refreshNetwork();
        loadMembers(server);
        for (Contractee contractee : contractees) {
            contractee.tickContract(this);
        }
        Level level = getLevel();
        if (host != null && host.isValid() && level != null) {
            for (PendingSpell spell : spells) {
                spell.fireReady(level.getGameTime());
            }
        }
    }

    private void loadMembers(MinecraftServer server) {
        contractees.clear();
        Set<UUID> loaded = getCastingMemberIds();
        for (UUID memberId : loaded) {
            Contractee member = findMember(server, memberId);
            if (member != null && member.valid()) {
                member.joinContract(this);
                contractees.add(member);
            }
        }
    }

    private static @Nullable Contractee findMember(MinecraftServer server, UUID memberId) {
        if (memberId.equals(WorldContractee.WORLD_ID)) {
            return WorldContractee.INSTANCE;
        }
        Contract contract = ContractSystem.INSTANCE.getContract(memberId);
        if (contract != null) {
            return contract;
        }
        Entity entity = EntityHelper.find(server, memberId);
        return entity == null ? null : EntitySubject.of(entity);
    }

    @Override
    public TermSet getTerms() {
        TermSet copy = new TermSet();
        copy.addAll(terms);
        return copy;
    }

    @Override
    public List<Contract> getActiveContractsList() {
        return activeContracts;
    }

    public CompoundTag write(CompoundTag tag) {
        tag.putString("type", getType());
        tag.putUUID("uuid", uuid);
        tag.putString("name", name);
        if (creatorId != null) {
            tag.putUUID("creator", creatorId);
        }
        ListTag members = new ListTag();
        for (UUID memberId : memberIds) {
            members.add(NbtUtils.createUUID(memberId));
        }
        tag.put("members", members);
        ListTag spellList = new ListTag();
        for (String incantation : incantations) {
            spellList.add(StringTag.valueOf(incantation));
        }
        tag.put("spells", spellList);
        CompoundTag colorTag = new CompoundTag();
        spellColors.forEach((incantation, colors) -> {
            if (incantations.contains(incantation)) {
                colorTag.putIntArray(incantation, colors.stream().mapToInt(Integer::intValue).toArray());
            }
        });
        tag.put("spell_colors", colorTag);
        ListTag logList = new ListTag();
        for (ContractAction action : log) {
            logList.add(action.write());
        }
        tag.put("log", logList);
        tag.put("terms", terms.write());
        tag.put("energy", stack.write(new CompoundTag()));
        tag.putDouble("imbued", imbued);
        return tag;
    }

    public void read(CompoundTag tag) {
        uuid = tag.getUUID("uuid");
        name = tag.getString("name");
        creatorId = tag.hasUUID("creator") ? tag.getUUID("creator") : null;
        memberIds.clear();
        for (Tag member : tag.getList("members", Tag.TAG_INT_ARRAY)) {
            memberIds.add(NbtUtils.loadUUID(member));
        }
        if (creatorId != null && !memberIds.contains(creatorId)) {
            memberIds.add(0, creatorId);
        }
        incantations.clear();
        ListTag spellList = tag.getList("spells", Tag.TAG_STRING);
        for (int i = 0; i < spellList.size(); i++) {
            incantations.add(spellList.getString(i));
        }
        spellColors.clear();
        CompoundTag colorTag = tag.getCompound("spell_colors");
        for (String incantation : colorTag.getAllKeys()) {
            spellColors.put(incantation, Arrays.stream(colorTag.getIntArray(incantation)).boxed().toList());
        }
        log.clear();
        ListTag logList = tag.getList("log", Tag.TAG_COMPOUND);
        for (int i = 0; i < logList.size(); i++) {
            log.add(ContractAction.read(logList.getCompound(i)));
        }
        terms.clear();
        terms.read(tag.getList("terms", Tag.TAG_COMPOUND));
        stack.read(tag.getCompound("energy"));
        imbued = tag.getDouble("imbued");
    }
}
