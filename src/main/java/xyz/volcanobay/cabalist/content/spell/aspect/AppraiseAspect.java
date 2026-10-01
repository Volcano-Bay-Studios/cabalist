package xyz.volcanobay.cabalist.content.spell.aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import xyz.volcanobay.cabalist.blockentity.ContractBlockEntity;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.core.CabalistSpatialNetworks;
import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.aspect.EnergyUse;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.energy.Lifeforce;
import xyz.volcanobay.cabalist.system.entropy.EntropyNetwork;
import xyz.volcanobay.cabalist.system.entropy.EntropyNetworkContract;
import xyz.volcanobay.cabalist.system.focus.Focus;
import xyz.volcanobay.cabalist.system.protection.ProtectionSystem;
import xyz.volcanobay.cabalist.system.request.NoticeSystem;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.HangingSpellSystem;
import xyz.volcanobay.cabalist.system.spell.PendingSpell;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.Subject;
import xyz.volcanobay.cabalist.system.visibility.HidingSystem;
import xyz.volcanobay.cabalist.util.BlockHelper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Tells the caster about what it's aimed at, as a notification circle kept up to date while the spell runs
public class AppraiseAspect extends Aspect {
    private static final int COLOR = 0x9FD7FF;
    private static final int MAX_LISTED = 6;
    private static final int UPDATE_TICKS = 5;

    @Override
    public Domain getDomain() {
        return CabalistSpellComponents.SOVEREIGN.get();
    }

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        out.clear();
    }

    @Override
    public boolean reachesWholeGroups() {
        return true;
    }

    @Override
    public boolean aimsWhereLooking() {
        return true;
    }

    @Override
    public boolean affectEntity(Entity entity, SpellClause clause, float magnitude) {
        List<String> lines = new ArrayList<>();
        if (entity instanceof LivingEntity living) {
            lines.add(String.format("health %.1f / %.1f", living.getHealth(), living.getMaxHealth()));
            lines.add(String.format("lifeforce %.1f", Lifeforce.get(living)));
        }
        List<PendingSpell> spells = HangingSpellSystem.INSTANCE.get(EntitySubject.of(entity));
        lines.add(spells.size() + " spells upon them");
        for (int i = 0; i < Math.min(MAX_LISTED, spells.size()); i++) {
            lines.add("  " + spells.get(i).getIncantation());
        }
        return tell(clause, entity.getUUID().toString(), entity.getName().getString(), lines, entity.getBoundingBox().getCenter());
    }

    @Override
    public boolean affectSpell(Spell spell, SpellClause clause, float magnitude) {
        if (clause.getCaster() instanceof EntitySubject caster) {
            spell.getActedOn().put(caster.getEntity().getUUID(), caster.getEntity().level().getGameTime());
        }
        List<String> lines = new ArrayList<>();
        lines.add(spell.getIncantation());
        lines.add(String.format("energy %.1f", spell.getRemainingAllotment()));
        lines.add(String.format("spending %.1f a second", spell.getLedger().getTotalRate()));
        return tell(clause, "spell" + System.identityHashCode(spell), spell.getName() == null ? "a spell" : spell.getName(), lines, getPosition(spell));
    }

    @Override
    public boolean affectContract(Contract contract, SpellClause clause, float magnitude) {
        if (clause.getCaster() instanceof EntitySubject caster && contract.getLevel() != null) {
            HidingSystem.INSTANCE.revealContract(contract.getUUID(), caster.getEntity().getUUID(), contract.getLevel().getGameTime());
            for (PendingSpell pending : contract.getPendingSpells()) {
                for (Spell running : pending.getRunningSpells()) {
                    running.getActedOn().put(caster.getEntity().getUUID(), contract.getLevel().getGameTime());
                }
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add("arbiter " + ContractSystem.INSTANCE.getContracteeName(contract.getCreatorId()));
        List<String> members = new ArrayList<>();
        List<String> subContracts = new ArrayList<>();
        for (UUID member : contract.getMemberIds()) {
            (ContractSystem.INSTANCE.getContract(member) != null ? subContracts : members).add(ContractSystem.INSTANCE.getContracteeName(member));
        }
        lines.add("members " + String.join(", ", members));
        if (!subContracts.isEmpty()) {
            lines.add("contracts within it " + String.join(", ", subContracts));
        }
        List<String> parents = new ArrayList<>();
        for (Contract parent : contract.getParentContracts()) {
            parents.add(ContractSystem.INSTANCE.getContracteeName(parent.getUUID()));
        }
        if (!parents.isEmpty()) {
            lines.add("within " + String.join(", ", parents));
        }
        Contract network = contract.getNetworkContract();
        if (!(contract instanceof EntropyNetworkContract) && network != null) {
            lines.add("on the network " + network.getDisplayName().getString());
            Set<UUID> networkMembers = new LinkedHashSet<>(network.getMemberIds());
            Contract authority = contract.getNetworkAuthority();
            if (authority != null) {
                networkMembers.addAll(authority.getMemberIds());
            }
            List<String> names = new ArrayList<>();
            for (UUID member : networkMembers) {
                if (ContractSystem.INSTANCE.getContract(member) == null) {
                    names.add(ContractSystem.INSTANCE.getContracteeName(member));
                }
            }
            lines.add("network members " + String.join(", ", names));
        }
        if (contract instanceof EntropyNetworkContract networkContract && networkContract.getNetwork() != null) {
            describeNetwork(networkContract.getNetwork(), lines);
        } else if (contract.getSettings().isMembersPay()) {
            lines.add("paid for by its members");
        } else if (contract.getNetworkContract() instanceof EntropyNetworkContract networkContract && networkContract.getNetwork() != null) {
            lines.add(String.format("drawing on a network with %.1f free", networkContract.getNetwork().getFreeEntropy()));
        } else {
            lines.add(String.format("energy %.1f of %.1f", contract.getEnergyStack().get(CabalistEnergyTypes.ENTROPY.get()), contract.getImbued()));
        }
        for (String line : contract.getIncantations()) {
            lines.add("  " + line);
        }
        String title = contract instanceof EntropyNetworkContract ? contract.getDisplayName().getString() : contract.getShownName();
        return tell(clause, contract.getUUID().toString(), title, lines, getPosition(contract));
    }

    @Override
    public boolean affectPosition(Level level, BlockPos pos, SpellClause clause, float magnitude) {
        Contract contract = ContractBlockEntity.getContractAt(level, pos);
        if (contract != null) {
            return affectContract(contract, clause, magnitude);
        }
        List<String> lines = new ArrayList<>();
        lines.add(pos.toShortString());
        if (clause.getCaster() instanceof EntitySubject caster && ProtectionSystem.INSTANCE.reveal(level, pos, caster.getEntity())) {
            lines.add("protected");
        }
        if (BlockHelper.isEntropetic(level.getBlockState(pos), pos, level)) {
            EntropyNetwork network = CabalistSpatialNetworks.ENTROPY_NETWORK.get(level).getNetwork(pos);
            if (network != null) {
                Contract networkContract = ContractSystem.INSTANCE.getNetworkContract(level.dimension(), network.getId());
                if (networkContract != null) {
                    lines.add("network " + networkContract.getDisplayName().getString());
                }
                describeNetwork(network, lines);
            }
        }
        return tell(clause, pos.toShortString(), level.getBlockState(pos).getBlock().getName().getString(), lines, pos.getCenter());
    }

    @Override
    public boolean affectItem(ItemStack stack, SpellClause clause, float magnitude) {
        Contract contract = ContractSystem.INSTANCE.getContract(stack);
        if (contract != null) {
            return affectContract(contract, clause, magnitude);
        }
        List<String> lines = new ArrayList<>();
        if (Focus.canHold(stack)) {
            lines.add(String.format("imbued lifeforce %.1f", Focus.get(stack).getTotalLifeforce()));
        }
        return tell(clause, "item" + System.identityHashCode(stack), stack.getHoverName().getString(), lines, getPosition(clause.getCaster()));
    }

    private static void describeNetwork(EntropyNetwork network, List<String> lines) {
        lines.add(String.format("producing %.2f a second", network.getProductionPerSecond()));
        lines.add(String.format("current %.1f of %.1f", network.getFreeEntropy(), network.getFreeEntropyCapacity()));
        lines.add(String.format("stored %.1f of %.1f", network.getStoredEntropy(), network.getStoredEntropyCapacity()));
        lines.add(String.format("losing %.2f a second on average", network.getAverageLossPerSecond()));
    }

    private static Vec3 getPosition(Subject subject) {
        Vector3d position = new Vector3d();
        subject.getPosition(position);
        return new Vec3(position.x, position.y, position.z);
    }

    private static boolean tell(SpellClause clause, String subjectKey, String title, List<String> lines, Vec3 about) {
        if (!(clause.getCaster() instanceof EntitySubject caster) || !(caster.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        Spell spell = clause.getSpell();
        if (spell == null) {
            NoticeSystem.INSTANCE.add(player.getUUID(), title, lines, COLOR, about);
            return true;
        }
        UUID id = UUID.nameUUIDFromBytes((System.identityHashCode(spell) + "/" + subjectKey).getBytes(StandardCharsets.UTF_8));
        if (NoticeSystem.INSTANCE.get(id) != null && player.level().getGameTime() % UPDATE_TICKS != 0) {
            return true;
        }
        NoticeSystem.INSTANCE.put(id, player.getUUID(), title, lines, COLOR, about, spell);
        return true;
    }
}
