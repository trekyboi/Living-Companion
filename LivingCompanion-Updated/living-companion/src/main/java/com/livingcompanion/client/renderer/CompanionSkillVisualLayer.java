package com.livingcompanion.client.renderer;

import com.livingcompanion.entity.CompanionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * Lightweight, client-side geometry for persistent companion skill effects.
 *
 * <p>Skill activation remains server-authoritative. This layer only reads the
 * synchronized visual flags from the companion and renders the corresponding
 * effect; it creates no entities and sends no gameplay packets.</p>
 */
public final class CompanionSkillVisualLayer
        extends RenderLayer<CompanionEntity, CompanionKnightModel> {

    private static final int SHIELD_WALL_FLAG = 1 << 1;
    private static final int SHIELD_COUNT = 6;

    public CompanionSkillVisualLayer(
            RenderLayerParent<CompanionEntity, CompanionKnightModel> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                       CompanionEntity companion, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if ((companion.getSkillVisualFlags() & SHIELD_WALL_FLAG) == 0) {
            return;
        }

        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        float time = ageInTicks + partialTicks;
        float orbit = time * 0.035F;
        float bob = (float) Math.sin(time * 0.12F) * 0.035F;

        for (int i = 0; i < SHIELD_COUNT; i++) {
            float angle = orbit + i * ((float) (Math.PI * 2.0D / SHIELD_COUNT));
            poseStack.pushPose();
            poseStack.translate(0.0D, 1.05D + bob + (i % 2 == 0 ? 0.06D : -0.06D), 0.0D);
            poseStack.mulPose(Axis.YP.rotation(angle));
            poseStack.translate(0.0D, 0.0D, -0.92D);
            poseStack.mulPose(Axis.XP.rotation(-0.08F));
            renderSpectralShield(poseStack, consumer, packedLight, time, i);
            poseStack.popPose();
        }
    }

    private static void renderSpectralShield(PoseStack poseStack, VertexConsumer consumer,
                                             int packedLight, float time, int index) {
        Matrix4f matrix = poseStack.last().pose();
        float pulse = 0.82F + (float) Math.sin(time * 0.16F + index) * 0.12F;
        int alpha = 105 + (int) (pulse * 35.0F);

        // Main translucent shield face.
        quad(consumer, matrix,
                -0.23F, 0.31F, 0.0F,  0.23F, 0.31F, 0.0F,
                 0.18F,-0.05F, 0.0F, -0.18F,-0.05F, 0.0F,
                42, 145, 255, alpha);
        // Pointed lower half.
        quad(consumer, matrix,
                -0.18F,-0.05F, 0.0F,  0.18F,-0.05F, 0.0F,
                 0.00F,-0.36F, 0.0F,  0.00F,-0.36F, 0.0F,
                28, 105, 220, alpha + 12);
        // Bright central inset gives the shield a layered, glass-like surface.
        quad(consumer, matrix,
                -0.12F, 0.19F,-0.006F, 0.12F, 0.19F,-0.006F,
                 0.09F,-0.035F,-0.006F,-0.09F,-0.035F,-0.006F,
                125, 210, 255, 105);

        // Luminous frame and central crest.
        bar(consumer, matrix, -0.25F, 0.33F, 0.25F, 0.33F, 0.018F, 120, 220, 255, 235);
        bar(consumer, matrix, -0.25F, 0.33F, 0.00F,-0.39F, 0.018F, 95, 195, 255, 230);
        bar(consumer, matrix,  0.25F, 0.33F, 0.00F,-0.39F, 0.018F, 95, 195, 255, 230);
        bar(consumer, matrix, -0.25F, 0.33F,-0.23F, 0.00F, 0.014F, 155, 230, 255, 210);
        bar(consumer, matrix,  0.25F, 0.33F, 0.23F, 0.00F, 0.014F, 155, 230, 255, 210);
        bar(consumer, matrix, -0.13F, 0.06F, 0.13F, 0.06F, 0.012F, 180, 235, 255, 220);
    }

    private static void quad(VertexConsumer out, Matrix4f matrix,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             int red, int green, int blue, int alpha) {
        vertex(out, matrix, x1, y1, z1, red, green, blue, alpha);
        vertex(out, matrix, x2, y2, z2, red, green, blue, alpha);
        vertex(out, matrix, x3, y3, z3, red, green, blue, alpha);
        vertex(out, matrix, x4, y4, z4, red, green, blue, alpha);
    }

    private static void bar(VertexConsumer out, Matrix4f matrix,
                            float x1, float y1, float x2, float y2, float width,
                            int red, int green, int blue, int alpha) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.0001F) return;
        float ox = -dy / length * width;
        float oy =  dx / length * width;
        quad(out, matrix,
                x1 + ox, y1 + oy, -0.012F,
                x2 + ox, y2 + oy, -0.012F,
                x2 - ox, y2 - oy, -0.012F,
                x1 - ox, y1 - oy, -0.012F,
                red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix,
                               float x, float y, float z,
                               int red, int green, int blue, int alpha) {
        out.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .endVertex();
    }
}
