package com.livingcompanion.ai;

import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** Keeps watch beside the owner while the owner sleeps through the night. */
public final class CompanionCampWatchGoal extends Goal {
    private final CompanionEntity companion;
    private int watchTicks;

    public CompanionCampWatchGoal(CompanionEntity companion) {
        this.companion = companion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        Player owner = companion.getOwnerPlayer();
        return owner != null && owner.isAlive() && owner.level() == companion.level()
                && owner.isSleeping() && companion.isAlive() && companion.onGround()
                && !companion.isInWater() && !companion.isPassenger()
                && companion.getTarget() == null && companion.distanceToSqr(owner) <= 64.0D;
    }

    @Override
    public boolean canContinueToUse() {
        Player owner = companion.getOwnerPlayer();
        return watchTicks > 0 && owner != null && owner.isAlive()
                && owner.level() == companion.level() && owner.isSleeping()
                && companion.getTarget() == null && companion.onGround()
                && companion.distanceToSqr(owner) <= 100.0D;
    }

    @Override
    public void start() {
        watchTicks = 240;
        companion.getNavigation().stop();
        companion.setPose(Pose.SITTING);
        companion.remember("kept watch while you slept");
        companion.commentOnEvent("Rest. I'll keep watch until morning.",
                "Sleep. I'll listen for anything approaching.",
                "I'll stay awake. We've earned a quiet night.");
    }

    @Override
    public void tick() {
        if (watchTicks > 0) watchTicks--;
    }

    @Override
    public void stop() {
        watchTicks = 0;
        if (companion.getPose() == Pose.SITTING) companion.setPose(Pose.STANDING);
    }
}
