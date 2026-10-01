package xyz.volcanobay.cabalist.system.aspect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.core.CabalistRenderSpecs;
import xyz.volcanobay.cabalist.system.contract.Contract;
import xyz.volcanobay.cabalist.system.render.RenderSpec;
import xyz.volcanobay.cabalist.system.spell.Domain;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellClause;
import xyz.volcanobay.cabalist.system.subject.*;

/**
 * What a spell does. Each hook returns whether it had an effect; unused magnitude costs nothing.
 */
public abstract class Aspect {
    private ModConfigSpec.BooleanValue enabled;
    private ModConfigSpec.DoubleValue magnitudeMultiplier;

    public final void defineConfig(ModConfigSpec.Builder builder) {
        enabled = builder.define("enabled", true);
        magnitudeMultiplier = builder.defineInRange("magnitude_multiplier", 1.0, 0, Double.MAX_VALUE);
        defineSettings(builder);
    }

    protected void defineSettings(ModConfigSpec.Builder builder) {
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public double getMagnitudeMultiplier() {
        return magnitudeMultiplier.get();
    }

    public abstract void getEnergyUse(Subject subject, SpellClause clause, float magnitude, EnergyUse out);

    public @Nullable Domain getDomain() {
        return null;
    }

    public RenderSpec getRenderSpec(SpellClause clause) {
        return CabalistRenderSpecs.get(this);
    }

    public boolean affectEntity(Entity entity, SpellClause clause, float magnitude) {
        return false;
    }

    public boolean affectPosition(Level level, BlockPos pos, SpellClause clause, float magnitude) {
        return false;
    }

    public boolean affectItem(ItemStack stack, SpellClause clause, float magnitude) {
        return false;
    }

    public boolean affectItem(ItemSubject item, SpellClause clause, float magnitude) {
        return affectItem(item.getStack(), clause, magnitude);
    }

    public boolean affectSpell(Spell spell, SpellClause clause, float magnitude) {
        return false;
    }

    public boolean affectContract(Contract contract, SpellClause clause, float magnitude) {
        return false;
    }

    public boolean affectRequest(RequestCircleSubject circle, SpellClause clause, float magnitude) {
        return false;
    }

    public boolean affectDraft(DraftCircleSubject circle, SpellClause clause, float magnitude) {
        return false;
    }

    public boolean affectNotice(NoticeCircleSubject circle, SpellClause clause, float magnitude) {
        return false;
    }

    public boolean aimsWhereLooking() {
        return false;
    }

    /**
     * True if groups like contracts receive this aspect themselves instead of passing it to their members.
     */
    public boolean reachesWholeGroups() {
        return false;
    }

    /**
     * False for aspects that act once, which a running spell doesn't repeat on its pulses.
     */
    public boolean isSustained() {
        return true;
    }

    /**
     * Aspects with a flat running cost pay nothing for their form being out
     */
    public boolean paysFormUpkeep() {
        return true;
    }

    /**
     * Whether reaching something shakes the screen.
     */
    public boolean shakesOnReach() {
        return true;
    }

    /**
     * When hidden, even its caster and members can't see it until it's appraised or acts
     */
    public boolean hidesFromOwners() {
        return false;
    }

    /**
     * Whether what it reaches counts as being acted on, which shows them a hidden spell
     */
    public boolean revealsToTarget() {
        return true;
    }
}
