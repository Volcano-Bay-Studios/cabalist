package xyz.volcanobay.cabalist.system.spell;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.core.CabalistTerms;
import xyz.volcanobay.cabalist.system.aspect.Aspect;
import xyz.volcanobay.cabalist.system.contract.Term;
import xyz.volcanobay.cabalist.system.form.Form;
import xyz.volcanobay.cabalist.system.form.FormDimension;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.ArrayList;
import java.util.List;

/**
 * A null target means the spell wanders.
 */
public class SpellClause {
    private final @Nullable SpellClause previous;
    private Subject caster;
    private Subject bearer;
    private @Nullable Subject target;
    private @Nullable Subject origin;
    private @Nullable Domain domain;
    private final List<Term> conditions = new ArrayList<>();
    private final List<List<Term>> requirementGroups = new ArrayList<>();
    private final List<List<String>> requirementPhrases = new ArrayList<>();
    private final List<Aspect> aspects = new ArrayList<>();
    private final List<Integer> colors = new ArrayList<>();
    private @Nullable Form form;
    private @Nullable Spell spell;
    private double spareNumber;
    private float power;
    private boolean lifeforceAllowed = true;
    private boolean isReleased;
    private boolean isBalance;
    private boolean isExhausted;
    private boolean isSustaining;
    private boolean isTargetInherited;
    private @Nullable String contractName;
    private @Nullable Subject contractMember;
    private final Devotion devotion = new Devotion();
    private @Nullable Subject location;
    private @Nullable List<SpellClause> continuation;
    private double formSize = Double.NaN;
    private double formBreadth = Double.NaN;
    private double formHeight = Double.NaN;
    private double sizeFactor = 1;
    private double breadthFactor = 1;
    private double heightFactor = 1;

    public SpellClause(Subject caster) {
        this.previous = null;
        this.caster = caster;
        this.bearer = caster;
    }

    public SpellClause(SpellClause previous) {
        this.previous = previous;
        this.caster = previous.caster;
        this.bearer = previous.bearer;
    }

    public void inheritFromPrevious() {
        if (previous == null) {
            return;
        }
        if (target == null) {
            target = previous.target;
            isTargetInherited = true;
        }
    }

    public boolean isTargetInherited() {
        return isTargetInherited;
    }

    public @Nullable SpellClause getPrevious() {
        return previous;
    }

    public Subject getCaster() {
        return caster;
    }

    public void setCaster(Subject caster) {
        this.caster = caster;
    }

    public Subject getBearer() {
        return bearer;
    }

    public void setBearer(Subject bearer) {
        this.bearer = bearer;
    }

    public @Nullable Subject getTarget() {
        return target;
    }

    public void setTarget(@Nullable Subject target) {
        this.target = target;
    }

    public @Nullable Subject getOrigin() {
        return origin;
    }

    public void setOrigin(@Nullable Subject origin) {
        this.origin = origin;
    }

    public @Nullable Domain getDomain() {
        return domain;
    }

    public void setDomain(@Nullable Domain domain) {
        this.domain = domain;
    }

    public List<Term> getConditions() {
        return conditions;
    }

    public void addCondition(Term condition) {
        conditions.add(condition);
    }

    public boolean runsAfterPrevious() {
        return hasCondition(CabalistTerms.AFTER_PREVIOUS.getId());
    }

    /**
     * Any group may be met, and every requirement in a group must be met.
     */
    public List<List<Term>> getRequirementGroups() {
        return requirementGroups;
    }

    public boolean hasRequirements() {
        return !requirementGroups.isEmpty();
    }

    public void addRequirement(Term requirement) {
        addRequirement(requirement, requirement.getResourceLocation().getPath().replace('_', ' '));
    }

public void addRequirement(Term requirement, String phrase) {
        if (requirementGroups.isEmpty()) {
            requirementGroups.add(new ArrayList<>());
            requirementPhrases.add(new ArrayList<>());
        }
        requirementGroups.get(requirementGroups.size() - 1).add(requirement);
        requirementPhrases.get(requirementPhrases.size() - 1).add(phrase);
    }

    public List<List<String>> getRequirementPhrases() {
        return requirementPhrases;
    }

    public void startRequirementGroup() {
        if (!requirementGroups.isEmpty() && !requirementGroups.get(requirementGroups.size() - 1).isEmpty()) {
            requirementGroups.add(new ArrayList<>());
            requirementPhrases.add(new ArrayList<>());
        }
    }

    public boolean hasCondition(ResourceLocation termLocation) {
        for (Term condition : conditions) {
            if (condition.getResourceLocation().equals(termLocation)) {
                return true;
            }
        }
        return false;
    }

    public List<Aspect> getAspects() {
        return aspects;
    }

    public void addAspect(Aspect aspect) {
        if (!aspects.contains(aspect)) {
            aspects.add(aspect);
        }
    }

    public @Nullable Form getForm() {
        return form;
    }

    public void setForm(@Nullable Form form) {
        this.form = form;
    }

    public @Nullable Spell getSpell() {
        return spell;
    }

    public void setSpell(Spell spell) {
        this.spell = spell;
    }

    public float getPower() {
        return power;
    }

    public void setPower(float power) {
        this.power = power;
    }

    /**
     * Where the clause is cast from. The caster unless a projectile carried the clause elsewhere.
     */
    public Subject getLocation() {
        return location != null ? location : caster;
    }

    public void setLocation(@Nullable Subject location) {
        this.location = location;
    }

    public @Nullable List<SpellClause> getContinuation() {
        return continuation;
    }

    public void setContinuation(@Nullable List<SpellClause> continuation) {
        this.continuation = continuation;
    }

    public Devotion getDevotion() {
        return devotion;
    }

    public @Nullable Domain getPrimaryDomain() {
        for (Aspect aspect : aspects) {
            if (aspect.getDomain() != null) {
                return aspect.getDomain();
            }
        }
        return null;
    }

    public @Nullable Subject getContractMember() {
        return contractMember;
    }

    public void setContractMember(@Nullable Subject contractMember) {
        this.contractMember = contractMember;
    }

    public @Nullable String getContractName() {
        return contractName;
    }

    public void setContractName(@Nullable String contractName) {
        this.contractName = contractName;
    }

    public boolean isExhausted() {
        return isExhausted;
    }

    public void markExhausted() {
        isExhausted = true;
    }

    public boolean isSustaining() {
        return isSustaining;
    }

    public void markSustaining() {
        isSustaining = true;
    }

    public boolean isBalance() {
        return isBalance;
    }

    public void markBalance() {
        isBalance = true;
    }

    public boolean isReleased() {
        return isReleased;
    }

    public void setReleased(boolean isReleased) {
        this.isReleased = isReleased;
    }

    public boolean isLifeforceAllowed() {
        return lifeforceAllowed;
    }

    public void setLifeforceAllowed(boolean lifeforceAllowed) {
        this.lifeforceAllowed = lifeforceAllowed;
    }

    public double getFormDimension(FormDimension dimension) {
        return switch (dimension) {
            case SIZE -> formSize;
            case BREADTH -> formBreadth;
            case HEIGHT -> formHeight;
        };
    }

    public void setFormDimension(FormDimension dimension, double value) {
        switch (dimension) {
            case SIZE -> formSize = value;
            case BREADTH -> formBreadth = value;
            case HEIGHT -> formHeight = value;
        }
    }

    public double getFormFactor(FormDimension dimension) {
        return switch (dimension) {
            case SIZE -> sizeFactor;
            case BREADTH -> breadthFactor;
            case HEIGHT -> heightFactor;
        };
    }

    public void multiplyFormFactor(FormDimension dimension, double factor) {
        switch (dimension) {
            case SIZE -> sizeFactor *= factor;
            case BREADTH -> breadthFactor *= factor;
            case HEIGHT -> heightFactor *= factor;
        }
    }

    public void addColor(int color) {
        if (!colors.contains(color)) {
            colors.add(color);
        }
    }

    public List<Integer> getColors() {
        return colors;
    }

    public double getSpareNumber() {
        return spareNumber;
    }

    public void addSpareNumber(double number) {
        spareNumber += number;
    }
}
