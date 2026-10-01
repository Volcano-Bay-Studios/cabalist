package xyz.volcanobay.cabalist.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.joml.Matrix4fc;
import xyz.volcanobay.cabalist.Cabalist;
import xyz.volcanobay.cabalist.client.casting.ClientCastingSystem;
import xyz.volcanobay.cabalist.client.renderer.glyph.GlyphRenderer;
import xyz.volcanobay.cabalist.client.request.DraftCircles;
import xyz.volcanobay.cabalist.client.request.NotificationCircles;
import xyz.volcanobay.cabalist.client.request.RequestReading;
import xyz.volcanobay.cabalist.client.spell.ClientHangingSpellSystem;
import xyz.volcanobay.cabalist.client.spell.ClientSpellVisualSystem;
import xyz.volcanobay.cabalist.client.spell.SpellShake;
import xyz.volcanobay.cabalist.client.tooltip.InscriptionTooltip;
import xyz.volcanobay.cabalist.client.visibility.ClientHidingSystem;
import xyz.volcanobay.cabalist.core.CabalistDataComponents;
import xyz.volcanobay.cabalist.mixin.client.ItemInHandRendererAccessor;
import xyz.volcanobay.cabalist.system.contract.Inscription;
import xyz.volcanobay.cabalist.system.focus.Focus;
import xyz.volcanobay.cabalist.system.focus.Imbuement;

import java.util.List;

@EventBusSubscriber(modid = Cabalist.MODID, value = Dist.CLIENT)
public class CabalistClientEvents {
    private static final float CHANNEL_ARM_RAISE = 0.4f;
    private static final float CHANNEL_ARM_SIDE = 0.1f;
    private static final float CHANNEL_ARM_OUTWARD = 0.5f;
    private static final float CHANNEL_ARM_TILT = -20;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        ClientSpellVisualSystem.INSTANCE.tick();
        ClientHangingSpellSystem.INSTANCE.tick();
        ClientHidingSystem.INSTANCE.tick();
        ClientCastingSystem.INSTANCE.tick();
        NotificationCircles.INSTANCE.tick();
        RequestReading.INSTANCE.tick();
        DraftCircles.INSTANCE.tick();
        SpellShake.tick();
    }

    // While channeling, the main hand is drawn raised and the beam leaves from it.
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        double channel = ClientSpellVisualSystem.INSTANCE.getChannel(player.getId(), event.getPartialTick());
        if (channel == 0) {
            return;
        }
        event.setCanceled(true);
        PoseStack pose = event.getPoseStack();
        float side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        pose.pushPose();
        pose.translate(-CHANNEL_ARM_SIDE * side * channel, CHANNEL_ARM_RAISE * channel, -CHANNEL_ARM_OUTWARD * channel); // pushes the arm when casting
        pose.mulPose(Axis.XP.rotationDegrees((float) (CHANNEL_ARM_TILT * channel)));
        ((ItemInHandRendererAccessor) Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()).cabalist$renderArmWithItem(
                player, event.getPartialTick(), event.getInterpolatedPitch(), InteractionHand.MAIN_HAND, 0, event.getItemStack(), 0,
                pose, event.getMultiBufferSource(), event.getPackedLight());
        pose.popPose();
    }

    @SubscribeEvent
    public static void onUseKey(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean isDrag = event.isUseItem() && player != null && (DraftCircles.INSTANCE.isDragging() || NotificationCircles.INSTANCE.isDragging()
                || player.isShiftKeyDown() && (DraftCircles.INSTANCE.tryStartDrag(player) || NotificationCircles.INSTANCE.tryStartDrag(player)));
        if (isDrag) {
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        boolean isCastControl = event.isUseItem() && player != null && ClientCastingSystem.isFocus(player.getMainHandItem());
        if (isCastControl || event.isAttack() && ClientCastingSystem.INSTANCE.isCasting()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        NotificationCircles.INSTANCE.onInteraction(event);
    }

    @SubscribeEvent
    public static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        Inscription inscription = event.getItemStack().get(CabalistDataComponents.INSCRIPTION.get());
        if (inscription != null && !inscription.lines().isEmpty() && Focus.canHold(event.getItemStack())) {
            event.getTooltipElements().add(Math.min(1, event.getTooltipElements().size()), Either.right(new InscriptionTooltip(inscription)));
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!Focus.canHold(stack)) {
            return;
        }
        List<Component> lines = event.getToolTip();
        int at = Math.min(1, lines.size());
        Imbuement imbuement = Focus.get(stack);
        if (imbuement.getTotalLifeforce() > 0) {
            lines.add(at, Component.literal(String.format("Lifeforce %.0f", imbuement.getTotalLifeforce())).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (stack.has(CabalistDataComponents.BLOOD.get())) {
            lines.add(at, Component.literal("Covered in blood").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
    }

    public static void onRenderLevel(VeilRenderLevelStageEvent.Stage stage, LevelRenderer levelRenderer, MultiBufferSource.BufferSource bufferSource, MatrixStack matrixStack,
                                     Matrix4fc frustumMatrix, Matrix4fc projectionMatrix, int renderTick, DeltaTracker deltaTracker, Camera camera, Frustum frustum) {
        if (stage != VeilRenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        ClientSpellVisualSystem.INSTANCE.render(matrixStack, bufferSource, camera, partialTick);
        ClientCastingSystem.INSTANCE.render(matrixStack, camera, partialTick);
        RequestReading.INSTANCE.render(matrixStack, camera, partialTick);
        DraftCircles.INSTANCE.render(matrixStack, camera, partialTick);
        GlyphRenderer.LOOSE.end();
    }
}
