package xyz.volcanobay.cabalist.client.request;

import net.minecraft.util.Mth;

public class CirclePlacement {
    protected float yaw;
    protected float height;
    private final float ease;
    private float shownYaw;
    private float shownHeight;
    private float prevShownYaw;
    private float prevShownHeight;

    public CirclePlacement(float yaw, float height, float ease) {
        this.ease = ease;
        this.yaw = shownYaw = prevShownYaw = yaw;
        this.height = shownHeight = prevShownHeight = height;
    }

    public void tick() {
        prevShownYaw = shownYaw;
        prevShownHeight = shownHeight;
        shownYaw += Mth.wrapDegrees(yaw - shownYaw) * ease;
        shownHeight = Mth.lerp(ease, shownHeight, height);
    }

    public void moveTo(float yaw, float height) {
        this.yaw = yaw;
        this.height = height;
    }

    public float getShownYaw(float partialTick) {
        return Mth.rotLerp(partialTick, prevShownYaw, shownYaw);
    }

    public float getShownHeight(float partialTick) {
        return Mth.lerp(partialTick, prevShownHeight, shownHeight);
    }
}
