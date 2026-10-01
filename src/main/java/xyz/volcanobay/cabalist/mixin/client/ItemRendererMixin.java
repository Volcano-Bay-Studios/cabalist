package xyz.volcanobay.cabalist.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.volcanobay.cabalist.client.renderer.item.LifeforceGlint;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderModelLists(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemStack;IILcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"))
    private void cabalist$renderLifeforceGlint(ItemRenderer renderer, BakedModel model, ItemStack stack, int light, int overlay, PoseStack poseStack,
                                               VertexConsumer buffer, Operation<Void> original, @Local(argsOnly = true) MultiBufferSource bufferSource) {
        original.call(renderer, model, stack, light, overlay, poseStack, buffer);
        VertexConsumer glint = LifeforceGlint.getBuffer(stack, bufferSource, poseStack.last());
        if (glint != null) {
            original.call(renderer, model, stack, light, overlay, poseStack, glint);
        }
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/BlockEntityWithoutLevelRenderer;renderByItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"))
    private void cabalist$renderCustomLifeforceGlint(BlockEntityWithoutLevelRenderer renderer, ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                                     MultiBufferSource bufferSource, int light, int overlay, Operation<Void> original) {
        original.call(renderer, stack, context, poseStack, LifeforceGlint.wrap(stack, bufferSource, poseStack.last()), light, overlay);
    }
}
