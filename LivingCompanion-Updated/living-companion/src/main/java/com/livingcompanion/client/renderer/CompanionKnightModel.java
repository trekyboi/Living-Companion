package com.livingcompanion.client.renderer;

import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Player-shaped companion model with three lightweight vanilla-rig melee
 * attack accents layered over Minecraft's normal arm-swing animation.
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
        float strength = (float) Math.sin(progress * Math.PI);
        if (strength <= 0.0F) {
            return;
        }

        boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
        ModelPart attackArm = rightHanded ? this.rightArm : this.leftArm;
        float side = rightHanded ? -1.0F : 1.0F;

        switch (entity.getAttackAnimationStyle()) {
            case 0 -> {
                // Horizontal slash: rotate the weapon arm across the body.
                attackArm.xRot -= 0.55F * strength;
                attackArm.yRot += side * 0.72F * strength;
                this.body.yRot += side * 0.20F * strength;
            }
            case 1 -> {
                // Overhead strike: lift the elbow and drive the arm downward.
                attackArm.xRot -= 0.95F * strength;
                attackArm.zRot += side * 0.14F * strength;
                this.body.xRot -= 0.10F * strength;
            }
            default -> {
                // Forward thrust: extend the weapon arm with a short torso lean.
                attackArm.xRot -= 0.42F * strength;
                attackArm.yRot += side * 0.48F * strength;
                this.body.xRot -= 0.16F * strength;
            }
        }
    }
}
