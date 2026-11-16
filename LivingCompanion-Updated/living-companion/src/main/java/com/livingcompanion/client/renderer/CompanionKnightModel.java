package com.livingcompanion.client.renderer;

import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Player-rig model for the companion.
 *
 * Better Mob Combat and Mob Player Animator supply attack keyframes at
 * runtime. This class leaves the vanilla PlayerModel animation pipeline
 * untouched so external animation layers can pose the knight's rig.
 */
public final class CompanionKnightModel extends PlayerModel<CompanionEntity> {

    public CompanionKnightModel(ModelPart root) {
        super(root, false);
    }
}
