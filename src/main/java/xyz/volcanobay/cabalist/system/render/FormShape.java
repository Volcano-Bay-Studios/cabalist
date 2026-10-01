package xyz.volcanobay.cabalist.system.render;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Where a form is and how long it lasts. Forms that follow an entity set its id; otherwise it is -1.
 * A beam channeled by an entity along its facing puts its reach in height, so clients can aim it themselves.
 */
public record FormShape(Kind kind, Vec3 start, Vec3 end, float radius, float height, int duration, int entityId) {
    public static final int NO_ENTITY = -1;

    public static final Codec<FormShape> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Kind.CODEC.fieldOf("kind").forGetter(FormShape::kind),
            Vec3.CODEC.fieldOf("start").forGetter(FormShape::start),
            Vec3.CODEC.fieldOf("end").forGetter(FormShape::end),
            Codec.FLOAT.fieldOf("radius").forGetter(FormShape::radius),
            Codec.FLOAT.fieldOf("height").forGetter(FormShape::height),
            Codec.INT.fieldOf("duration").forGetter(FormShape::duration),
            Codec.INT.fieldOf("entity").forGetter(FormShape::entityId)
    ).apply(instance, FormShape::new));

    public static FormShape point(Vec3 at, int duration) {
        return new FormShape(Kind.POINT, at, at, 0.5f, 0, duration, NO_ENTITY);
    }

    public static FormShape following(Kind kind, int entityId, Vec3 start, float radius, float height) {
        return new FormShape(kind, start, start, radius, height, 0, entityId);
    }

    public boolean followsEntity() {
        return entityId != NO_ENTITY;
    }

    public enum Kind implements StringRepresentable {
        POINT,
        BEAM,
        AREA,
        PROJECTILE,
        SKY_STRIKE;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase();
        }
    }
}
