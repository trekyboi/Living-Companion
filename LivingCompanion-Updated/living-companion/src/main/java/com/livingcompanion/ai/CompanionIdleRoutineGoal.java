package com.livingcompanion.ai;

import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Gives the companion brief resting pauses when the area is calm.
 *
 * <p>The routine yields immediately to movement, combat, water, and owner
 * commands. Vanilla look goals remain free to make the companion scan around
 * while seated.</p>
 */
public final class CompanionIdleRoutineGoal extends Goal {
    private static final double OWNER_IDLE_RANGE_SQR = 64.0D;
    private static final int MIN_REST_TICKS = 80;
    private static final int REST_TICK_VARIANCE = 81;
    private static final int START_CHANCE = 1200;

    private final CompanionEntity companion;
    private int restTicks;

    public CompanionIdleRoutineGoal(CompanionEntity companion) {
        this.companion = companion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return isCalmAndNearby()
                && companion.getRandom().nextInt(START_CHANCE) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return restTicks > 0 && isCalmAndNearby();
    }

    @Override
    public void start() {
        restTicks = MIN_REST_TICKS + companion.getRandom().nextInt(REST_TICK_VARIANCE);
        companion.getNavigation().stop();
        companion.setPose(Pose.SITTING);

        if (companion.getRandom().nextInt(3) == 0) {
            companion.commentOnEvent(
                    "I'll take a moment here. Keep an eye on the road.",
                    "A short rest. Even a knight needs to ease his feet.",
                    "I'll watch the surroundings while we catch our breath."
            );
        }
    }

    @Override
    public void tick() {
        if (restTicks > 0) {
            restTicks--;
        }
    }

    @Override
    public void stop() {
        restTicks = 0;
        if (companion.getPose() == Pose.SITTING) {
            companion.setPose(Pose.STANDING);
        }
    }

    private boolean isCalmAndNearby() {
        if (!companion.isAlive()
                || companion.getTarget() != null
                || !companion.onGround()
                || companion.isInWater()
                || companion.isPassenger()
                || !companion.getNavigation().isDone()) {
            return false;
        }

        if (companion.getCommand() == CompanionEntity.Command.STAY) {
            return true;
        }

        Player owner = companion.getOwnerPlayer();
        return owner != null
                && owner.isAlive()
                && owner.level() == companion.level()
                && companion.distanceToSqr(owner) <= OWNER_IDLE_RANGE_SQR;
    }
}
