package xyz.volcanobay.cabalist.content.spell.aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import xyz.volcanobay.cabalist.blockentity.ContractBlockEntity;
import xyz.volcanobay.cabalist.core.CabalistTerms;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.aspect.EnergyUse;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.contract.ContractSystem;
import xyz.volcanobay.cabalist.system.spell.HangingSpellSystem;
import xyz.volcanobay.cabalist.system.spell.PendingSpell;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.spell.TriggerEvent;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.function.BiConsumer;

// Sets off whatever is waiting "when released", on one spell or on every spell of a contract.
public class ReleaseAspect extends Aspect {

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        out.clear();
    }

    @Override
    public boolean aimsWhereLooking() {
        return true;
    }

    @Override
    public boolean reachesWholeGroups() {
        return true;
    }

    @Override
    public boolean isSustained() {
        return false;
    }

    @Override
    public boolean shakesOnReach() {
        return false;
    }

    @Override
    public boolean affectSpell(Spell spell, SpellClause clause, float magnitude) {
        PendingSpell pending = HangingSpellSystem.INSTANCE.find(spell);
        if (pending == null || !pending.isAwaiting(CabalistTerms.RELEASED.getId())) {
            return false;
        }
        return raiseLater(pending.getHost(), clause, pending::onTrigger);
    }

    @Override
    public boolean affectContract(Contract contract, SpellClause clause, float magnitude) {
        Subject host = contract.getHost();
        if (host == null) {
            return false;
        }
        for (PendingSpell pending : contract.getPendingSpells()) {
            if (pending.isAwaiting(CabalistTerms.RELEASED.getId())) {
                return raiseLater(host, clause, contract::onTrigger);
            }
        }
        return false;
    }

    @Override
    public boolean affectPosition(Level level, BlockPos pos, SpellClause clause, float magnitude) {
        Contract contract = ContractBlockEntity.getContractAt(level, pos);
        return contract != null && affectContract(contract, clause, magnitude);
    }

    @Override
    public boolean affectItem(ItemStack stack, SpellClause clause, float magnitude) {
        Contract contract = ContractSystem.INSTANCE.getContract(stack);
        return contract != null && affectContract(contract, clause, magnitude);
    }

    private static boolean raiseLater(Subject host, SpellClause clause, BiConsumer<TriggerEvent, Long> raise) {
        if (!(host.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        TriggerEvent event = new TriggerEvent(CabalistTerms.RELEASED.getId(), host, clause.getCaster());
        MinecraftServer server = level.getServer();
        server.tell(new TickTask(server.getTickCount(), () -> raise.accept(event, level.getGameTime())));
        return true;
    }
}
