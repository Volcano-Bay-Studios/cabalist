package xyz.volcanobay.cabalist.client.casting;

import foundry.veil.api.client.render.MatrixStack;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.volcanobay.cabalist.client.renderer.circle.CircleLayout;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircle;
import xyz.volcanobay.cabalist.client.renderer.circle.MagicCircleRenderer;
import xyz.volcanobay.cabalist.client.renderer.glyph.Glyph;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphRenderer;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphStyle;
import xyz.volcanobay.cabalist.client.renderer.glyph.Glyphs;
import xyz.volcanobay.cabalist.system.casting.CastingPhase;
import xyz.volcanobay.cabalist.system.render.Palette;
import xyz.volcanobay.cabalist.util.ColorHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// One caster's glyphs and the circle they're building. Glyphs live in the world around the caster.
public class CastingScene {
    // do not touch my constants
    private static final float TEXT_DISTANCE = 1.5f;
    // The circle sits farther away as it grows so it stays about the same size in view.
    private static final float CIRCLE_DISTANCE_BASE = 0.6f;
    private static final float CIRCLE_DISTANCE_PER_RADIUS = 2.2f;
    private static final float CIRCLE_DISTANCE_EASE = 0.1f;
    private static final float CIRCLE_APPEAR_TICKS = 10;
    private static final float HEAD_Y = -0.22f;
    private static final float SPOKEN_Y = -0.05f;
    private static final float EDGE_X = 1.6f;
    private static final float FLOAT_MIN_RADIUS = 1.0f;
    private static final float FLOAT_MAX_RADIUS = 2.5f;
    private static final float FLOAT_BOTTOM = -1.6f;
    private static final float FLOAT_MIN_TOP = -0.3f;
    private static final float FLOAT_MAX_TOP = 1.4f;
    private static final float FLOAT_CELL = 0.08f;
    private static final float HEAD_CELL = 0.1f;
    private static final float LANDING_DISTANCE = 1;
    private static final float RISE_PER_TICK = 0.03f;
    private static final float CHARS_PER_TICK = 1;
    private static final float CHARGING_RAMP = 0.3f;
    private static final float CHARGING_ALPHA = 0.35f;
    private static final float FLOAT_ALPHA = 0.6f;
    private static final float CHARGING_BRIGHTNESS = 0.5f;
    private static final float COLOR_EASE = 0.15f;
    private static final float HEAD_WHITENESS = 0.5f;
    private static final float HEAD_SPEED = 0.25f;
    private static final float CIRCLE_SPEED = 0.18f;
    private static final float ARRIVED = 0.05f;
    private static final float GRAVITY = 0.012f;
    private static final float BOB_SPEED = 0.12f;
    private static final float BOB_SPREAD = 0.7f;
    private static final float BOB_HEIGHT = 0.012f;
    private static final int END_TICKS = 15;
    private static final float TEAR_PER_TICK = 0.5f;
    private static final float TEAR_PER_TICK_AT_END = 3;
    private static final float SHATTER_PROGRESS = 0.8f;
    private static final int SHATTER_CHARS = 30;
    private static final float SHED_PER_TICK = 3;
    private static final float SHED_SPEED = 0.06f;
    private static final float SHED_FADE_PER_TICK = 1 / 20f;
    private static final float PUSH_PER_TICK = 2;
    private static final float PUSH_SPEED = 0.15f;
    private static final float PUSH_ARRIVED = 0.3f;
    private static final float PROMPT_Y = 0.32f;
    private static final float ANSWER_Y = 0.16f;
    private static final int VALID_ANSWER = 0x8CFF7A;
    private static final int INVALID_ANSWER = 0x9A9A9A;
    private static final int WHITE = 0xFFFFFF;
    private static final int RED = 0xFF3A2A;
    private static final CastingBasis FALLBACK = CastingBasis.of(Vec3.ZERO, new Vec3(0, 0, 1));

    private final int entityId;
    private final RandomSource random = RandomSource.create();
    private final List<SceneChar> chars = new ArrayList<>();
    private final List<SceneChar> head = new ArrayList<>();
    private CastingPhase phase = CastingPhase.CHARGING;
    private List<String> words = List.of();
    private String current = "";
    private String spoken = "";
    private Palette palette = Palette.EMPTY;
    private float gathered;
    private float capacity;
    private float rate;
    private boolean isFrozen;
    private float charge;
    private float aim;
    private float summonDebt;
    private int age;
    private int chargedAt = -1;
    private int endedAt = -1;
    private boolean isCast;
    private float circlePhase;
    private float prevCirclePhase;
    private float circlePulse;
    private float circleDistance = -1;
    private float prevCircleDistance;
    private float colorWeight;
    private @Nullable CastingBasis view;
    private boolean isDismissing;
    private Vec3 dismissAt = Vec3.ZERO;
    private float dismiss;
    private float tearDebt;
    private float shedDebt;
    private @Nullable AABB lastTearBox;
    private boolean isPushing;
    private Vec3 pushAt = Vec3.ZERO;
    private float pushDebt;
    private @Nullable Vec3 pushTo;
    private float prevCharge;
    private final List<SceneChar> prompt = new ArrayList<>();
    private final List<SceneChar> answer = new ArrayList<>();
    private String answerText = "";
    private boolean isAnswerValid;

    public CastingScene(int entityId) {
        this.entityId = entityId;
    }

    public int getEntityId() {
        return entityId;
    }

    public CastingPhase getPhase() {
        return phase;
    }

    public float getAim() {
        return aim;
    }

    public boolean isEnding() {
        return endedAt >= 0;
    }

    public boolean isFinished() {
        return isEnding() && age - endedAt > END_TICKS && chars.isEmpty();
    }

    public @Nullable CastingBasis getView() {
        return view;
    }

    public void setStatus(CastingPhase phase, Palette palette, float gathered, float capacity, float rate, boolean frozen, float charge, float aim, String spoken) {
        if (phase != CastingPhase.CHARGING && chargedAt < 0) {
            chargedAt = age;
        }
        this.phase = phase;
        this.palette = palette;
        this.gathered = gathered;
        this.capacity = capacity;
        this.rate = rate;
        this.isFrozen = frozen;
        this.charge = charge;
        this.aim = aim;
        this.spoken = spoken;
    }

    public boolean isDismissing() {
        return isDismissing;
    }

    public Vec3 getDismissAt() {
        return dismissAt;
    }

    // Something that was nearly torn apart when it's dropped has just shattered
    public void setDismiss(boolean dismissing, Vec3 at, float progress) {
        boolean isChanged = dismissing != isDismissing || at.distanceToSqr(dismissAt) > 0.01;
        if (isChanged && dismiss >= SHATTER_PROGRESS && lastTearBox != null) {
            for (int i = 0; i < SHATTER_CHARS; i++) {
                tearFrom(lastTearBox);
            }
        }
        if (isChanged) {
            lastTearBox = null;
        }
        isDismissing = dismissing;
        dismissAt = at;
        dismiss = progress;
    }

    public boolean isPushing() {
        return isPushing;
    }

    public Vec3 getPushAt() {
        return pushAt;
    }

    public void setPush(boolean pushing, Vec3 at) {
        isPushing = pushing;
        pushAt = at;
    }

    public boolean isPrompting() {
        return !prompt.isEmpty();
    }

    // The prompt is spelled out with glyphs pulled from the scene.
    public void showPrompt(String text) {
        releasePrompt();
        float[] slots = Glyphs.layout(text, HEAD_CELL);
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != ' ') {
                SceneChar character = claim(text.charAt(i));
                character.state = State.PROMPT;
                character.lineX = slots[i];
                prompt.add(character);
            }
        }
    }

    public void setAnswerValid(boolean valid) {
        isAnswerValid = valid;
    }

    public void setAnswer(String text) {
        int keep = 0;
        while (keep < answerText.length() && keep < text.length() && answerText.charAt(keep) == text.charAt(keep)) {
            keep++;
        }
        while (answer.size() > keep) {
            answer.remove(answer.size() - 1).fall(random);
        }
        for (int i = keep; i < text.length(); i++) {
            SceneChar character = claim(text.charAt(i));
            character.state = State.ANSWER;
            answer.add(character);
        }
        float[] slots = Glyphs.layout(text, HEAD_CELL);
        for (int i = 0; i < answer.size(); i++) {
            answer.get(i).lineX = slots[i];
        }
        answerText = text;
    }

    // The prompt's glyphs go back to drifting up around the caster.
    public void closePrompt() {
        releasePrompt();
        setAnswer("");
    }

    private void releasePrompt() {
        for (SceneChar character : prompt) {
            character.state = State.FLOATING;
            character.isLatin = false;
            character.vy = 0;
            character.fadeY = (float) character.position.y + 0.3f + random.nextFloat() * 0.5f;
        }
        prompt.clear();
    }

    public void end(boolean cast) {
        if (isEnding()) {
            return;
        }
        endedAt = age;
        isCast = cast;
        for (SceneChar character : chars) {
            if (cast && character.state == State.HEAD) {
                sendToCircle(character, words.size(), -1);
            } else if (cast) {
                character.state = State.LEAVING;
            } else {
                character.fall(random);
            }
        }
        head.clear();
        prompt.clear();
        answer.clear();
    }

    public boolean isEmpty() {
        return words.isEmpty() && current.isEmpty();
    }

    // Animates the difference between what was written and what is written now.
    public void setText(List<String> newWords, String newCurrent) {
        if (isEnding()) {
            return;
        }
        int common = 0;
        while (common < words.size() && common < newWords.size() && words.get(common).equals(newWords.get(common))) {
            common++;
        }
        boolean committed = newWords.size() == words.size() + 1 && common == words.size() && newWords.get(common).equals(current);
        boolean unfreed = newWords.size() == words.size() - 1 && common == newWords.size() && current.isEmpty();
        String headText = current;
        if (committed) {
            int start = getLetterStart(newWords, words.size());
            for (int i = 0; i < head.size(); i++) {
                sendToCircle(head.get(i), words.size(), getLetter(current, start, i));
            }
            head.clear();
            headText = "";
        } else if (unfreed) {
            String word = words.get(words.size() - 1);
            int start = getLetterStart(words, words.size() - 1);
            for (int i = 0; i < word.length(); i++) {
                SceneChar character = spawnOnCircle(word.charAt(i), getLetter(word, start, i));
                character.state = State.HEAD;
                head.add(character);
            }
            headText = word;
        } else {
            for (int i = common; i < newWords.size(); i++) {
                String word = newWords.get(i);
                int start = getLetterStart(newWords, i);
                for (int j = 0; j < word.length(); j++) {
                    sendToCircle(claim(word.charAt(j)), i, getLetter(word, start, j));
                }
            }
        }
        if (newCurrent.startsWith(headText)) {
            for (int i = headText.length(); i < newCurrent.length(); i++) {
                addToHead(claim(newCurrent.charAt(i)));
            }
        } else {
            int keep = headText.startsWith(newCurrent) ? newCurrent.length() : 0;
            while (head.size() > keep) {
                head.remove(head.size() - 1).fall(random);
            }
            for (int i = keep; i < newCurrent.length(); i++) {
                addToHead(claim(newCurrent.charAt(i)));
            }
        }
        words = List.copyOf(newWords);
        current = newCurrent;
    }

    private void addToHead(SceneChar character) {
        character.state = State.HEAD;
        head.add(character);
    }


    /**
     * @param view A point in view coordinates, relative to the origin.
     * @param tearBox where the spell being dismissed is
     * @param pushTo where energy is being poured, relative to the caster's eyes
     */
    public void tick(@Nullable CastingBasis view, @Nullable AABB tearBox, @Nullable Vec3 pushTo) {
        if (view != null) {
            this.view = view;
        }
        prevCharge = charge;
        if (tearBox != null) {
            lastTearBox = tearBox;
            tearDebt += Mth.lerp(dismiss, TEAR_PER_TICK, TEAR_PER_TICK_AT_END);
            while (tearDebt >= 1) {
                tearDebt--;
                tearFrom(tearBox);
            }
        }
        this.pushTo = isEnding() ? null : pushTo;
        if (this.pushTo != null) {
            pushDebt += PUSH_PER_TICK;
            while (pushDebt >= 1) {
                pushDebt--;
                pushOne();
            }
        }
        CastingBasis basis = getBasis();
        age++;
        prevCirclePhase = circlePhase;
        circlePhase += 1 + gathered / 100f;
        circlePulse = Math.max(0, circlePulse - 0.08f);
        easeColor();
        float targetDistance = getTargetCircleDistance();
        prevCircleDistance = circleDistance < 0 ? targetDistance : circleDistance;
        circleDistance = Mth.lerp(CIRCLE_DISTANCE_EASE, prevCircleDistance, targetDistance);
        if (!isEnding() && !isFrozen) {
            float ramp = phase == CastingPhase.CHARGING ? CHARGING_RAMP + (1 - CHARGING_RAMP) * charge : 1;
            summonDebt += CHARS_PER_TICK * rate * ramp;
            while (summonDebt >= 1) {
                summonDebt--;
                spawnFloating();
            }
        }
        if (phase == CastingPhase.AIMING && !isEnding()) {
            shedDebt += SHED_PER_TICK * aim * aim;
            while (shedDebt >= 1) {
                shedDebt--;
                shed(basis);
            }
        }
        for (int i = 0; i < head.size(); i++) {
            head.get(i).slot = i;
        }
        float[] slots = layoutHead();
        for (Iterator<SceneChar> iterator = chars.iterator(); iterator.hasNext(); ) {
            SceneChar character = iterator.next();
            character.remember();
            character.age++;
            if (!tick(character, slots, basis)) {
                iterator.remove();
            }
        }
    }

    private boolean tick(SceneChar character, float[] slots, CastingBasis basis) {
        switch (character.state) {
            case FLOATING -> { // this is rising around
                float speed = isFrozen ? 0 : RISE_PER_TICK * rate * (0.6f + 0.4f * character.seed);
                character.vy = Mth.lerp(0.1f, character.vy, speed);
                character.position = character.position.add(
                        Mth.sin(character.age * 0.1f + character.seed * 6) * 0.003f * rate, character.vy, Mth.cos(character.age * 0.08f + character.seed * 5) * 0.003f * rate);
                float fadeIn = Math.min(1, character.age / 6f);
                float fadeOut = Mth.clamp((character.fadeY - (float) character.position.y) / 0.2f, 0, 1);
                character.alpha = (phase == CastingPhase.CHARGING ? CHARGING_ALPHA : FLOAT_ALPHA) * fadeIn * fadeOut;
                return character.position.y < character.fadeY;
            }
            case HEAD -> { // typing
                int slot = character.slot;
                float x = slot < slots.length ? slots[slot] : 0;
                float y = HEAD_Y + Mth.sin(age * BOB_SPEED + slot * BOB_SPREAD) * BOB_HEIGHT;
                Vec3 target = basis.offset(x, y, TEXT_DISTANCE);
                character.moveTowards(target, HEAD_SPEED);
                if (character.position.distanceTo(target) < ARRIVED * 2) {
                    character.isLatin = true;
                }
                character.alpha = Math.min(1, character.alpha + 0.15f);
                return true;
            }
            case PROMPT, ANSWER -> { // asking something, usually to dismiss
                float y = character.state == State.PROMPT ? PROMPT_Y : ANSWER_Y;
                Vec3 target = basis.offset(character.lineX, y + Mth.sin(age * BOB_SPEED + character.lineX * 8) * BOB_HEIGHT, TEXT_DISTANCE);
                character.moveTowards(target, HEAD_SPEED);
                if (character.position.distanceTo(target) < ARRIVED * 2) {
                    character.isLatin = true;
                }
                character.alpha = Math.min(1, character.alpha + 0.15f);
                return true;
            }
            case TO_CIRCLE -> { // moving into the circle after the word is finished
                character.isLatin = false;
                Vec3 center = basis.offset(0, 0, circleDistance);
                MagicCircleRenderer.RuneSlot slot = character.letter < 0 ? null
                        : MagicCircleRenderer.getRuneSlot(getCircle(center, basis), String.join(" ", words), character.letter, circlePhase);
                Vec3 target = slot == null ? center.add(basis.right().scale(character.circleX * getCircleRadius()))
                        .add(basis.up().scale(character.circleY * getCircleRadius())) : center.add(slot.offset());
                character.moveTowards(target, CIRCLE_SPEED);
                double remaining = character.position.distanceTo(target);
                character.landing = (float) Mth.clamp(1 - remaining / LANDING_DISTANCE, 0, 1);
                if (slot != null) {
                    character.runeRight = slot.right();
                    character.runeUp = slot.up();
                }
                character.alpha = Math.min(1, character.alpha + 0.1f);
                if (remaining < ARRIVED) {
                    circlePulse = 1;
                    return false;
                }
                return true;
            }
            case TO_TARGET -> {
                if (pushTo == null) {
                    character.state = State.LEAVING;
                    return true;
                }
                character.moveTowards(pushTo, PUSH_SPEED);
                character.alpha = Math.min(1, character.alpha + 0.1f);
                return character.position.distanceTo(pushTo) > PUSH_ARRIVED;
            }
            case FALLING -> {
                character.vy -= GRAVITY;
                character.position = character.position.add(character.vx, character.vy, character.vz);
                character.alpha -= 1 / 24f;
                return character.alpha > 0;
            }
            case SHED -> { // when casting with held right click
                character.position = character.position.add(character.vx, character.vy, character.vz);
                character.alpha -= SHED_FADE_PER_TICK;
                return character.alpha > 0;
            }
            case LEAVING -> {
                character.position = character.position.add(0, 0.01, 0);
                character.alpha -= 0.1f;
                return character.alpha > 0;
            }
        }
        return false;
    }

    // Faint white while charging, then the spell's color, easing between them.
    private void easeColor() {
        colorWeight = Mth.lerp(COLOR_EASE, colorWeight, phase == CastingPhase.CHARGING ? 0 : 1);
    }

    private int getShownColor(float position) {
        return ColorHelper.lerp(WHITE, palette.sample(position, GlyphRenderer.getTime()), colorWeight);
    }

    private Palette getShownPalette() {
        List<Integer> colors = new ArrayList<>();
        for (int color : palette.or(Palette.of(palette.getPrimary())).colors()) {
            colors.add(0xFF000000 | ColorHelper.lerp(WHITE, color, colorWeight));
        }
        return new Palette(colors);
    }

    private CastingBasis getBasis() {
        return view == null ? FALLBACK : view;
    }

    // Slot centers for the word at the write head, in x.
    private float[] layoutHead() {
        StringBuilder text = new StringBuilder();
        for (SceneChar character : head) {
            text.append(character.character);
        }
        return Glyphs.layout(text.toString(), HEAD_CELL);
    }

    // Glyphs rise all around the caster, from their feet to somewhere above their head.
    private void spawnFloating() {
        SceneChar character = new SceneChar((char) ('a' + random.nextInt(26)), random.nextFloat());
        float angle = random.nextFloat() * Mth.TWO_PI;
        float radius = Mth.lerp(random.nextFloat(), FLOAT_MIN_RADIUS, FLOAT_MAX_RADIUS);
        character.position = new Vec3(Mth.cos(angle) * radius, FLOAT_BOTTOM + random.nextFloat() * 0.3f, Mth.sin(angle) * radius);
        character.fadeY = Mth.lerp(random.nextFloat(), FLOAT_MIN_TOP, FLOAT_MAX_TOP);
        character.remember();
        chars.add(character);
    }

    private void pushOne() {
        SceneChar pushed = null;
        for (SceneChar character : chars) {
            if (character.state == State.FLOATING) {
                pushed = character;
                break;
            }
        }
        if (pushed == null) {
            spawnFloating();
            pushed = chars.get(chars.size() - 1);
        }
        pushed.state = State.TO_TARGET;
    }

    // An aimed circle throws glyphs off its rim, more as the cast gets ready.
    private void shed(CastingBasis basis) {
        float angle = random.nextFloat() * Mth.TWO_PI;
        Vec3 outward = basis.right().scale(Mth.cos(angle)).add(basis.up().scale(Mth.sin(angle)));
        SceneChar character = new SceneChar((char) ('a' + random.nextInt(26)), random.nextFloat());
        character.state = State.SHED;
        character.position = basis.offset(0, 0, getCircleDistance()).add(outward.scale(getCircleRadius()));
        Vec3 velocity = outward.scale(SHED_SPEED * (0.5f + random.nextFloat()));
        character.vx = velocity.x;
        character.vy = velocity.y;
        character.vz = velocity.z;
        character.alpha = 1;
        character.remember();
        chars.add(character);
    }

    // Glyphs torn off a spell fall red, like backspaced ones.
    private void tearFrom(AABB box) {
        SceneChar character = new SceneChar((char) ('a' + random.nextInt(26)), random.nextFloat());
        character.position = new Vec3(Mth.lerp(random.nextDouble(), box.minX, box.maxX), Mth.lerp(random.nextDouble(), box.minY, box.maxY),
                Mth.lerp(random.nextDouble(), box.minZ, box.maxZ));
        character.remember();
        character.fall(random);
        chars.add(character);
    }

    // Pulls the matching glyph nearest the write head out of the floating ones, or brings one in from the edge of view.
    private SceneChar claim(char wanted) {
        Vec3 headAt = getBasis().offset(0, HEAD_Y, TEXT_DISTANCE);
        SceneChar best = null;
        double bestDistance = Double.MAX_VALUE;
        for (SceneChar character : chars) {
            if (character.state == State.FLOATING && character.character == wanted) {
                double distance = character.position.distanceToSqr(headAt);
                if (distance < bestDistance) {
                    best = character;
                    bestDistance = distance;
                }
            }
        }
        if (best != null) {
            best.vy = 0;
            return best;
        }
        SceneChar character = new SceneChar(wanted, random.nextFloat());
        character.position = getBasis().offset((random.nextBoolean() ? 1 : -1) * EDGE_X, (random.nextFloat() * 2 - 1) * 0.5f, TEXT_DISTANCE);
        character.remember();
        chars.add(character);
        return character;
    }

    private SceneChar spawnOnCircle(char wanted, int letter) {
        SceneChar character = new SceneChar(wanted, random.nextFloat());
        CastingBasis basis = getBasis();
        Vec3 center = basis.offset(0, 0, getCircleDistance());
        MagicCircleRenderer.RuneSlot slot = letter < 0 ? null : MagicCircleRenderer.getRuneSlot(getCircle(center, basis), String.join(" ", words), letter, circlePhase);
        character.position = slot == null ? center : center.add(slot.offset());
        character.alpha = 1;
        character.remember();
        chars.add(character);
        return character;
    }

    // letter is where it's written in the circle's words, or -1 to land anywhere on the rim.
    private void sendToCircle(SceneChar character, int word, int letter) {
        float angle = random.nextFloat() * Mth.TWO_PI;
        character.state = State.TO_CIRCLE;
        character.word = word;
        character.letter = letter;
        character.circleX = Mth.cos(angle);
        character.circleY = Mth.sin(angle);
    }

    // Where a word starts in the circle's text, which runs its words together with single spaces.
    private static int getLetterStart(List<String> words, int word) {
        String before = CircleLayout.normalize(String.join(" ", words.subList(0, Math.min(word, words.size()))));
        return before.isEmpty() ? 0 : before.length() + 1;
    }

    // Only words that are all letters are written into the circle letter for letter.
    private static int getLetter(String word, int start, int index) {
        return CircleLayout.normalize(word).equals(word) ? start + index : -1;
    }

    private MagicCircle getCircle(Vec3 center, CastingBasis basis) {
        return new MagicCircle(center, basis.forward().scale(-1), getCircleRadius());
    }

    private float getCircleRadius() {
        return CircleLayout.getRadiusFor(String.join(" ", words)) / 16f;
    }

    private float getTargetCircleDistance() {
        return CIRCLE_DISTANCE_BASE + CIRCLE_DISTANCE_PER_RADIUS * getCircleRadius();
    }

    private float getCircleDistance() {
        return circleDistance < 0 ? getTargetCircleDistance() : circleDistance;
    }

    // Words are only written into the circle once all of their glyphs have arrived.
    private int getLandedWords() {
        int landed = words.size();
        for (SceneChar character : chars) {
            if (character.state == State.TO_CIRCLE && character.word < landed) {
                landed = Math.max(0, character.word);
            }
        }
        return landed;
    }

    // World glyphs face the viewer; the write head and anything headed for the circle face the caster like the circle does.
    // origin is the caster's eyes relative to the camera.
    public void render(GlyphRenderer glyphs, MatrixStack pose, Vec3 origin, CastingBasis viewBasis, Vec3 cameraRight, Vec3 cameraUp,
                       boolean viewLayer, float partialTick) {
        int index = -1;
        for (SceneChar character : chars) {
            index++;
            boolean isViewChar = character.state == State.HEAD || character.state == State.TO_CIRCLE
                    || character.state == State.PROMPT || character.state == State.ANSWER;
            if (isViewChar != viewLayer) {
                continue;
            }
            float alpha = Mth.lerp(partialTick, character.prevAlpha, character.alpha);
            if (alpha <= 0.01f) {
                continue;
            }
            int shown = getShownColor(character.seed);
            int color = switch (character.state) {
                case FLOATING, LEAVING, TO_CIRCLE, TO_TARGET, SHED -> shown;
                case HEAD, PROMPT -> ColorHelper.lerp(shown, WHITE, HEAD_WHITENESS);
                case ANSWER -> isAnswerValid ? VALID_ANSWER : INVALID_ANSWER;
                case FALLING -> RED;
            };
            float cell = character.state == State.HEAD || character.state == State.PROMPT || character.state == State.ANSWER ? HEAD_CELL : FLOAT_CELL;
            Vec3 at = origin.add(character.previous.lerp(character.position, partialTick));
            Vec3 right = isViewChar ? viewBasis.right() : cameraRight;
            Vec3 up = isViewChar ? viewBasis.up() : cameraUp;
            if (character.state == State.TO_CIRCLE) {
                cell = Mth.lerp(character.landing, HEAD_CELL, MagicCircleRenderer.getRuneCell());
                if (character.runeRight != null) {
                    right = right.lerp(character.runeRight, character.landing).normalize();
                    up = up.lerp(character.runeUp, character.landing).normalize();
                }
            }
            glyphs.glyph(pose, glyphOf(character), at, right, up, cell, ColorHelper.withAlpha(color, alpha), index * GlyphRenderer.SPREAD);
        }
        if (viewLayer && !spoken.isEmpty() && !isEnding()) {
            renderSpoken(glyphs, pose, origin, viewBasis, partialTick);
        }
    }

    // What's being said but not yet heard in full, as faint text above the write head.
    private void renderSpoken(GlyphRenderer glyphs, MatrixStack pose, Vec3 origin, CastingBasis basis, float partialTick) {
        float[] slots = Glyphs.layout(spoken, FLOAT_CELL);
        for (int i = 0; i < spoken.length(); i++) {
            float bob = Mth.sin((age + partialTick) * BOB_SPEED + i * BOB_SPREAD) * BOB_HEIGHT;
            glyphs.glyph(pose, Glyph.latin(spoken.charAt(i)), origin.add(basis.offset(slots[i], SPOKEN_Y + bob, TEXT_DISTANCE)), basis.right(), basis.up(),
                    FLOAT_CELL, ColorHelper.withAlpha(ColorHelper.lerp(getShownColor((float) i / spoken.length()), WHITE, HEAD_WHITENESS), 0.5f), i * GlyphRenderer.SPREAD);
        }
    }

    public void collectCircle(Vec3 origin, CastingBasis basis, float partialTick, List<MagicCircleRenderer.CircleDraw> out) {
        float time = age + partialTick;
        float fade = Math.min(1, time / CIRCLE_APPEAR_TICKS);
        float distance = Mth.lerp(partialTick, prevCircleDistance, getCircleDistance());
        if (isEnding()) {
            float progress = Mth.clamp((time - endedAt) / END_TICKS, 0, 1);
            fade = 1 - progress;
            if (isCast) {
                distance += progress * progress * 8;
            }
        }
        float fill = capacity <= 0 ? 0 : Mth.clamp(gathered / capacity, 0, 1);
        float readyPulse = chargedAt < 0 ? 0 : Math.max(0, 1 - (time - chargedAt) / 10f);
        float brightness = 0.5f + 0.35f * fill + 0.4f * Math.max(circlePulse, readyPulse) + 0.3f * aim;
        if (phase == CastingPhase.CHARGING) {
            brightness *= CHARGING_BRIGHTNESS;
        }
        // Builds quickly at first, then slows as it nears the threshold.
        float build = 1;
        if (phase == CastingPhase.CHARGING) {
            float shown = 1 - Mth.lerp(partialTick, prevCharge, charge);
            build = 1 - shown * shown * shown;
        }
        Vec3 center = origin.add(basis.offset(0, 0, distance));
        String landed = String.join(" ", words.subList(0, getLandedWords()));
        out.add(new MagicCircleRenderer.CircleDraw(getCircle(center, basis), entityId & MagicCircle.SEED_MASK,
                Mth.lerp(partialTick, prevCirclePhase, circlePhase), fade, brightness, getShownPalette(), landed, build, ""));
    }

    private static Glyph glyphOf(SceneChar character) {
        boolean latin = character.isLatin || !Character.isLetter(character.character);
        return new Glyph(latin ? GlyphStyle.LATIN : GlyphStyle.RUNE, character.character);
    }

    private enum State {
        FLOATING,
        HEAD,
        TO_CIRCLE,
        TO_TARGET,
        PROMPT,
        ANSWER,
        FALLING,
        SHED,
        LEAVING
    }

    private static class SceneChar {
        private final char character;
        private final float seed;
        private State state = State.FLOATING;
        private boolean isLatin; // latin or glyph
        private Vec3 position = Vec3.ZERO;
        private Vec3 previous = Vec3.ZERO;
        private double vx, vy, vz;
        private float circleX, circleY;
        private int word = -1;
        private int letter = -1;
        private float landing;
        private float lineX;
        private @Nullable Vec3 runeRight;
        private @Nullable Vec3 runeUp;
        private float alpha, prevAlpha;
        private float fadeY;
        private int age;
        private int slot;

        private SceneChar(char character, float seed) {
            this.character = character;
            this.seed = seed;
        }

        private void remember() {
            previous = position;
            prevAlpha = alpha;
        }

        private void moveTowards(Vec3 target, float speed) {
            position = position.lerp(target, speed);
        }

        private void fall(RandomSource random) {
            state = State.FALLING;
            vx = (random.nextFloat() - 0.5f) * 0.01f;
            vz = (random.nextFloat() - 0.5f) * 0.01f;
            vy = random.nextFloat() * 0.02f;
            alpha = Math.max(alpha, 0.8f);
        }
    }
}
