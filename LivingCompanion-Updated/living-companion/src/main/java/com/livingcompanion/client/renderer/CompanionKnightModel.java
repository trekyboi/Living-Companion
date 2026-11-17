package com.livingcompanion.client.renderer;

import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;

/**
 * Player-rig model for the companion. Adds restrained equipment-aware idle poses;
 * combat animation remains delegated to the normal vanilla / Better Mob Combat pipeline.
 */
public final class CompanionKnightModel extends PlayerModel<CompanionEntity> {

    public CompanionKnightModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void setupAnim(CompanionEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        if (entity.getPose() == net.minecraft.world.entity.Pose.SITTING
                || entity.getTarget() != null || entity.isUsingItem()
                || entity.getDeltaMovement().horizontalDistanceSqr() > 0.0025D
                || limbSwingAmount > 0.08F) {
            return;
        }

        ItemStack mainHand = entity.getItemBySlot(EquipmentSlot.MAINHAND);
        ItemStack offHand = entity.getItemBySlot(EquipmentSlot.OFFHAND);
        boolean rightDominant = entity.getMainArm() == HumanoidArm.RIGHT;
        float breath = (float) Math.sin(ageInTicks * 0.08F) * 0.025F;
        float dominantSign = rightDominant ? 1.0F : -1.0F;

        boolean hasBow = mainHand.getItem() instanceof BowItem || offHand.getItem() instanceof BowItem
                || mainHand.getItem() instanceof CrossbowItem || offHand.getItem() instanceof CrossbowItem;
        boolean hasShield = mainHand.getItem() instanceof ShieldItem || offHand.getItem() instanceof ShieldItem;
        boolean hasSword = mainHand.getItem() instanceof SwordItem;

        if (hasBow) {
            this.rightArm.xRot = -0.82F + breath;
            this.leftArm.xRot = -0.82F - breath;
            this.rightArm.yRot = -0.18F;
            this.leftArm.yRot = 0.18F;
        } else if (hasShield) {
            ModelPart shieldArm = offHand.getItem() instanceof ShieldItem
                    ? (rightDominant ? this.leftArm : this.rightArm)
                    : (rightDominant ? this.rightArm : this.leftArm);
            shieldArm.xRot = -0.58F + breath;
            shieldArm.yRot = dominantSign * 0.12F;
            shieldArm.zRot = dominantSign * 0.18F;
            ModelPart weaponArm = rightDominant ? this.rightArm : this.leftArm;
            weaponArm.xRot = -0.12F - breath;
        } else if (hasSword) {
            ModelPart weaponArm = rightDominant ? this.rightArm : this.leftArm;
            weaponArm.xRot = -0.18F + breath;
            weaponArm.yRot = dominantSign * -0.08F;
            weaponArm.zRot = dominantSign * 0.06F;
        } else {
            this.rightArm.xRot += breath;
            this.leftArm.xRot -= breath;
        }
    }
}
