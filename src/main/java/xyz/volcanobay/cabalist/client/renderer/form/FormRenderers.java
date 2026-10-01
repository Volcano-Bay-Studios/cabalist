package xyz.volcanobay.cabalist.client.renderer.form;

import xyz.volcanobay.cabalist.system.render.FormShape;

import java.util.EnumMap;
import java.util.Map;

public class FormRenderers {
    private static final Map<FormShape.Kind, FormRenderer> RENDERERS = new EnumMap<>(FormShape.Kind.class);

    static {
        register(FormShape.Kind.POINT, new PointRenderer());
        register(FormShape.Kind.BEAM, new BeamRenderer());
        register(FormShape.Kind.AREA, new AreaRenderer());
        register(FormShape.Kind.PROJECTILE, new ProjectileRenderer());
        register(FormShape.Kind.SKY_STRIKE, new SkyStrikeRenderer());
    }

    public static void register(FormShape.Kind kind, FormRenderer renderer) {
        RENDERERS.put(kind, renderer);
    }

    public static FormRenderer get(FormShape.Kind kind) {
        return RENDERERS.get(kind);
    }
}
