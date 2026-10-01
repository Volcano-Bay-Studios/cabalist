package xyz.volcanobay.cabalist.content.spell.aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.core.CabalistSpellComponents;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.aspect.EnergyUse;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.protection.ProtectionSystem;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

// Its cost is a flat rate per column it holds, whatever it stops. Keeps the ground it reaches from being broken, built on, used, blown up or burned by anyone but the caster and their members
public class ProtectAspect extends Aspect {
    private static final Map<Spell, Allowed> ALLOWED = Collections.synchronizedMap(new WeakHashMap<>());

    private ModConfigSpec.DoubleValue costPerColumnPerSecond;

    // One oscilistone keeps about ten columns claimed.
    @Override
    protected void defineSettings(ModConfigSpec.Builder builder) {
        costPerColumnPerSecond = builder.defineInRange("cost_per_column_per_second", 0.0267, 0, Double.MAX_VALUE);
    }

    @Override
    public Domain getDomain() {
        return CabalistSpellComponents.SOVEREIGN.get();
    }

    @Override
    public void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out) {
        out.set(CabalistEnergyTypes.ENTROPY.get(), -costPerColumnPerSecond.get() / 20);
    }

    @Override
    public boolean paysFormUpkeep() {
        return false;
    }

    @Override
    public boolean shakesOnReach() {
        return false;
    }

    @Override
    public boolean hidesFromOwners() {
        return true;
    }

    @Override
    public boolean revealsToTarget() {
        return false;
    }

    @Override
    public boolean affectPosition(Level level, BlockPos pos, SpellClause clause, float magnitude) {
        return ProtectionSystem.INSTANCE.protect(level, pos, getAllowed(clause, level), clause.getSpell());
    }

    @Override
    public boolean affectEntity(Entity entity, SpellClause clause, float magnitude) {
        return ProtectionSystem.INSTANCE.protect(entity.level(), entity.blockPosition(), getAllowed(clause, entity.level()), clause.getSpell());
    }

    // A contract's spell lets in whoever counts as its member there, including parents' and its network authority's.
    // Worked out once a tick per spell, however many columns it holds.
    public static Set<UUID> getAllowed(SpellClause clause, Level level) {
        Spell spell = clause.getSpell();
        if (spell != null) {
            Allowed cached = ALLOWED.get(spell);
            if (cached != null && cached.at() == level.getGameTime()) {
                return cached.ids();
            }
        }
        Set<UUID> allowed = new HashSet<>();
        Subject host = spell == null ? clause.getCaster() : spell.getHost();
        if (host != null && host.getUUID() != null) {
            allowed.add(host.getUUID());
        }
        Contract contract = host == null ? null : host.getBoundContract();
        if (contract != null) {
            allowed.addAll(contract.getCastingMemberIds());
        }
        if (spell != null) {
            ALLOWED.put(spell, new Allowed(level.getGameTime(), allowed));
        }
        return allowed;
    }

    private record Allowed(long at, Set<UUID> ids) {
    }
}
