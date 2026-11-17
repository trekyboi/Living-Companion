package com.livingcompanion.entity;

import com.livingcompanion.ai.FollowOwnerGoal;
import com.livingcompanion.ai.CompanionPatrolGoal;
import com.livingcompanion.ai.CompanionCampWatchGoal;
import com.livingcompanion.ai.CompanionIdleRoutineGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Mage companion. Uses the shared ownership, inventory, progression, relationship,
 * and awareness systems, but maintains distance and casts class skills instead of
 * rushing into melee.
 */
public final class MageCompanionEntity extends CompanionEntity {
    public MageCompanionEntity(EntityType<? extends MageCompanionEntity> type, Level level) {
        super(type, level);
        this.setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.10D));
        this.goalSelector.addGoal(2, new CompanionPatrolGoal(this));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(4, new CompanionCampWatchGoal(this));
        this.goalSelector.addGoal(5, new CompanionIdleRoutineGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class,
                10, true, false, target -> target.distanceToSqr(this)
                <= net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE.getDefaultValue()
                * net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE.getDefaultValue()));
    }
}
