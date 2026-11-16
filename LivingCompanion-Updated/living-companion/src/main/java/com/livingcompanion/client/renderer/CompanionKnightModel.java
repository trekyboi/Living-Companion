package com.livingcompanion.client.renderer;

import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Player-shaped companion model with three visible vanilla-rig melee attack
 * motions layered over Minecraft's normal arm-swing animation.
 */
public final class CompanionKnightModel extends PlayerModel<CompanionEntity> {

    public CompanionKnightModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void setupAnim(CompanionEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        int remaining = entity.getAttackAnimationTicks();
        if (remaining <= 0) {
            return;
        }

        float progress = 1.0F - (remaining / (float) CompanionEntity.ATTACK_ANIMATION_DURATION);
        // A broad, readable attack arc with a sharp follow-through.
        float strength = (float) Math.sin(progress * Math.PI);
        float followThrough = (float) Math.sin(progress * Math.PI * 0.5F);
        if (strength <= 0.0F) {
            return;
        }

        boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
        ModelPart attackArm = rightHanded ? this.rightArm : this.leftArm;
        float side = rightHanded ? -1.0F : 1.0F;

        switch (entity.getAttackAnimationStyle()) {
            case 0 -> {
                // Horizontal slash: rotate the weapon arm across the body.
                attackArm.xRot -= 0.95F * strength;
                attackArm.yRot += side * 1.05F * strength;
                attackArm.zRot += side * 0.18F * strength;
                this.body.yRot += side * 0.32F * strength;
            }
            case 1 -> {
                // Overhead strike: lift the elbow and drive the arm downward.
                attackArm.xRot -= 1.35F * strength;
                attackArm.zRot += side * 0.22F * strength;
                this.body.xRot -= 0.22F * strength;
            }
            default -> {
                // Forward thrust: extend the weapon arm with a short torso lean.
                attackArm.xRot -= 0.82F * strength;
                attackArm.yRot += side * 0.22F * strength;
                this.body.xRot -= 0.28F * strength;
                this.body.yRot += side * 0.10F * followThrough;
            }
        }
    }
}
