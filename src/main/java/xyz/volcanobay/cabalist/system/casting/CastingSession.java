package xyz.volcanobay.cabalist.system.casting;

import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.core.CabalistConfig;
import xyz.volcanobay.cabalist.core.CabalistEnergyTypes;
import xyz.volcanobay.cabalist.networking.packet.CastingEndS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.CastingStateS2CPacket;
import xyz.volcanobay.cabalist.networking.packet.LookTargetC2SPacket;
import xyz.volcanobay.cabalist.system.energy.EnergyStack;
import xyz.volcanobay.cabalist.system.focus.Focus;
import xyz.volcanobay.cabalist.system.focus.Gathering;
import xyz.volcanobay.cabalist.system.render.Palette;
import xyz.volcanobay.cabalist.system.render.SpellVisuals;
import xyz.volcanobay.cabalist.system.request.ConsentWords;
import xyz.volcanobay.cabalist.system.request.PartyStatus;
import xyz.volcanobay.cabalist.system.spell.EnergyLedger;
import xyz.volcanobay.cabalist.system.spell.HangingSpellSystem;
import xyz.volcanobay.cabalist.system.spell.Spell;
import xyz.volcanobay.cabalist.system.spell.SpellEngine;
import xyz.volcanobay.cabalist.system.spell.SpellExecutor;
import xyz.volcanobay.cabalist.system.subject.DraftCircleSubject;
import xyz.volcanobay.cabalist.system.subject.EntitySubject;
import xyz.volcanobay.cabalist.system.subject.NoticeCircleSubject;
import xyz.volcanobay.cabalist.system.subject.RequestCircleSubject;
import xyz.volcanobay.cabalist.system.subject.Subject;

import java.util.ArrayList;
import java.util.List;

// A spell being built with a focus. Glyphs are summoned while it gathers, and the ones never used still add a little capacity
public class CastingSession {
    private static final double TICK_SECONDS = 0.05;
    private static final double CHARS_PER_SECOND = 8;
    private static final double CHARGING_CHAR_RAMP = 0.3;
    public static final double AIM_SECONDS = 0.75;
    private static final int SYNC_INTERVAL_TICKS = 5;
    public static final int ANSWER_NONE = 0;
    public static final int ANSWER_YES = 1;
    public static final int ANSWER_NO = 2;

    private final ServerPlayer player;
    private final ItemStack focusStack;
    private final Focus focus;
    private final Gathering gathering;
    private final double chargeTime;
    private final List<String> words = new ArrayList<>();
    private String current = "";
    private String spoken = "";
    private CastingPhase phase = CastingPhase.CHARGING;
    private double chargeSeconds;
    private double aimSeconds;
    private double charsSummoned;
    private int ackSeq;
    private Palette palette = Palette.EMPTY;
    private int ticks;
    private boolean isDirty = true;
    private LookTargetC2SPacket dismissRequest = LookTargetC2SPacket.NOTHING;
    private double dismissSeconds;
    private double drawBacklash;
    private LookTargetC2SPacket lookTarget = LookTargetC2SPacket.NOTHING;
    private boolean isPushing;
    private @Nullable Vec3 pushAt;
    private String answer = "";
    private int verdict = ANSWER_NONE;

    public CastingSession(ServerPlayer player, Focus focus) {
        this.player = player;
        this.focusStack = player.getMainHandItem();
        this.focus = focus;
        this.gathering = new Gathering(focus, null);
        this.chargeTime = focus.getChargeSeconds();
    }

    public CastingPhase getPhase() {
        return phase;
    }

    public boolean tick() {
        if (player.isRemoved() || !player.isAlive() || player.getMainHandItem() != focusStack) {
            end(false);
            return false;
        }
        ticks++;
        if (phase == CastingPhase.CHARGING || phase == CastingPhase.CHARGED) {
            chargeSeconds += TICK_SECONDS;
            if (phase == CastingPhase.CHARGING && getCharge() >= 1) {
                phase = CastingPhase.CHARGED;
                isDirty = true;
            }
        }
        if (phase == CastingPhase.AIMING) {
            aimSeconds += TICK_SECONDS;
        }
        double ramp = phase == CastingPhase.CHARGING ? CHARGING_CHAR_RAMP + (1 - CHARGING_CHAR_RAMP) * getCharge() : 1;
        charsSummoned += CHARS_PER_SECOND * gathering.getRateFraction() * ramp * TICK_SECONDS;
        gathering.setChars(getCharsUsed(), getCharsFaded());
        gathering.tick(TICK_SECONDS);
        tickDismissal();
        tickPushing();
        if (ticks % 20 == 0 && drawBacklash > 0) {
            player.hurt(player.damageSources().magic(), (float) (drawBacklash / CabalistConfig.ENTROPY_PER_HEALTH.get()));
            drawBacklash = 0;
        }
        if (isDirty || phase == CastingPhase.CHARGING || ticks % SYNC_INTERVAL_TICKS == 0) {
            sync();
        }
        return true;
    }

    public void hold() {
        if (phase == CastingPhase.WRITING && getText().isEmpty()) {
            isPushing = true;
        } else if (phase == CastingPhase.WRITING) {
            phase = CastingPhase.AIMING;
            aimSeconds = 0;
            isDirty = true;
        }
    }

    public boolean release() {
        if (isPushing) {
            isPushing = false;
            return true;
        }
        switch (phase) {
            case CHARGING -> {
                end(false);
                return false;
            }
            case CHARGED -> phase = CastingPhase.WRITING;
            case AIMING -> {
                if (aimSeconds >= AIM_SECONDS) {
                    cast();
                    return false;
                }
                phase = CastingPhase.WRITING;
            }
            default -> {
            }
        }
        isDirty = true;
        return true;
    }

    public void setDismissTarget(LookTargetC2SPacket target) {
        if (!target.isSameTarget(dismissRequest)) {
            dismissSeconds = 0;
            isDirty = true;
        }
        dismissRequest = target;
    }

    private void tickDismissal() {
        Subject target = phase != CastingPhase.CHARGING ? LookTargets.resolve(player, dismissRequest) : null;
        boolean isTargetable = target instanceof Spell || target instanceof RequestCircleSubject || target instanceof DraftCircleSubject
                || target instanceof NoticeCircleSubject;
        if (!isTargetable) {
            if (dismissSeconds > 0) {
                dismissSeconds = 0;
                isDirty = true;
            }
            return;
        }
        double duration = CabalistConfig.STANDARD_CHARGE_SECONDS.get();
        if (target instanceof Spell spell && spell.hasOwnEnergy()) {
            double share = Math.min(1, TICK_SECONDS / Math.max(TICK_SECONDS, duration - dismissSeconds));
            EnergyStack stack = spell.getEnergyStack();
            double taken = stack.extract(CabalistEnergyTypes.ENTROPY.get(), stack.get(CabalistEnergyTypes.ENTROPY.get()) * share);
            spell.getLedger().spend(EnergyLedger.DISMISSED, taken);
            gathering.draw(taken);
            double safe = gathering.getThroughput() * CabalistConfig.DRAW_SAFE_THROUGHPUT.get() * TICK_SECONDS;
            drawBacklash += Math.max(0, taken - safe);
        }
        dismissSeconds += TICK_SECONDS;
        if (dismissSeconds < duration) {
            return;
        }
        if (target instanceof Spell spell && !HangingSpellSystem.INSTANCE.removeSpell(spell)) {
            spell.dismiss();
        } else if (target instanceof RequestCircleSubject circle) {
            circle.answer(PartyStatus.DISPELLED, player.getUUID());
        } else if (target instanceof DraftCircleSubject circle) {
            circle.remove(true, EntitySubject.of(player));
        } else if (target instanceof NoticeCircleSubject circle) {
            circle.dismiss(player.getUUID());
        }
        dismissRequest = LookTargetC2SPacket.NOTHING;
        dismissSeconds = 0;
        isDirty = true;
    }

    private void tickPushing() {
        Spell target = isPushing && getText().isEmpty() && LookTargets.resolve(player, lookTarget) instanceof Spell spell && spell.hasOwnEnergy() ? spell : null;
        if (target == null) {
            if (pushAt != null) {
                pushAt = null;
                isDirty = true;
            }
            return;
        }
        target.feed(gathering.push(gathering.getGathered() * Math.min(1, TICK_SECONDS / CabalistConfig.STANDARD_CHARGE_SECONDS.get())));
        if (pushAt == null) {
            isDirty = true;
        }
        pushAt = lookTarget.at();
    }

    public LookTargetC2SPacket getLookTarget() {
        return lookTarget;
    }

    public void setLookTarget(LookTargetC2SPacket lookTarget) {
        this.lookTarget = lookTarget;
    }

    public void answer(String text) {
        answer = text;
        verdict = judge(text);
        isDirty = true;
    }

    private static int judge(String text) {
        return switch (ConsentWords.judge(text)) {
            case YES -> ANSWER_YES;
            case NO -> ANSWER_NO;
            default -> ANSWER_NONE;
        };
    }

    public void edit(int edit, int seq, String text) {
        ackSeq = Math.max(ackSeq, seq);
        if (!phase.isWriting() && phase != CastingPhase.CHARGED) {
            return;
        }
        StringBuilder typed = new StringBuilder();
        for (char character : text.toLowerCase().toCharArray()) {
            if (CastingEdit.isTypeable(character)) {
                typed.append(character);
            }
        }
        current = CastingEdit.apply(edit, typed.toString(), words, current);
        onTextChanged();
    }

    public void speak(String text, boolean isFinal) {
        if (!phase.isWriting() && phase != CastingPhase.CHARGED) {
            return;
        }
        if (isFinal) {
            for (String token : SpellEngine.INSTANCE.tokenize(text)) {
                words.add(token);
            }
            spoken = "";
        } else {
            spoken = String.join(" ", SpellEngine.INSTANCE.tokenize(text));
        }
        onTextChanged();
    }

    private void onTextChanged() {
        String text = getText();
        if (!text.isEmpty()) {
            Spell spell = SpellEngine.INSTANCE.resolve(text, EntitySubject.of(player), 0);
            gathering.setDomain(Gathering.getDomain(spell));
            gathering.setCapacityMultiplier(spell.getCapacityMultiplier());
        }
        palette = words.isEmpty() ? Palette.EMPTY
                : SpellVisuals.getPalette(SpellEngine.INSTANCE.resolve(String.join(" ", words), EntitySubject.of(player), 0));
        isDirty = true;
    }

    public void cast() {
        String text = getText();
        if (text.isEmpty()) {
            end(false);
            return;
        }
        Spell spell = SpellEngine.INSTANCE.resolve(text, EntitySubject.of(player), gathering.getSeconds());
        gathering.setChars(getCharsUsed(), getCharsFaded());
        gathering.applyTo(spell, player);
        end(true);
        SpellExecutor.INSTANCE.cast(spell);
    }

    public void end(boolean cast) {
        VeilPacketManager.trackingAndSelf(player).sendPacket(new CastingEndS2CPacket(player.getId(), cast));
    }

    private void sync() {
        isDirty = false;
        VeilPacketManager.trackingAndSelf(player).sendPacket(new CastingStateS2CPacket(player.getId(), phase.ordinal(), List.copyOf(words), current, spoken, palette,
                (float) gathering.getGathered(), (float) gathering.getCapacity(), (float) gathering.getRateFraction(), phase.isWriting() && gathering.isFull(),
                (float) getCharge(), (float) Math.min(1, aimSeconds / AIM_SECONDS),
                dismissSeconds > 0, dismissRequest.at(), (float) (dismissSeconds / CabalistConfig.STANDARD_CHARGE_SECONDS.get()),
                pushAt != null, pushAt == null ? Vec3.ZERO : pushAt, answer, verdict, ackSeq));
    }

    private String getText() {
        List<String> all = new ArrayList<>(words);
        if (!current.isEmpty()) {
            all.add(current);
        }
        return String.join(" ", all);
    }

    private double getCharge() {
        return Math.min(1, chargeSeconds / chargeTime);
    }

    private int getCharsUsed() {
        return getText().replace(" ", "").length();
    }

    private int getCharsFaded() {
        return (int) Math.max(0, charsSummoned - getCharsUsed());
    }
}
