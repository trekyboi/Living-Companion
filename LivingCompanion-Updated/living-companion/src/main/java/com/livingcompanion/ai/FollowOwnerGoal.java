package com.livingcompanion.ai;

import com.livingcompanion.config.CompanionConfig;
import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/** Keeps the companion near its owner unless the owner has ordered him to stay. */
public final class FollowOwnerGoal extends Goal {
    private final CompanionEntity companion;
    private final double speed;
    private Player owner;
    private long nextTeleportTime;

    public FollowOwnerGoal(CompanionEntity companion, double speed) {
        this.companion = companion;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (companion.getCommand() == CompanionEntity.Command.STAY) return false;
        owner = companion.getOwnerPlayer();
        if (owner == null || !owner.isAlive() || owner.level() != companion.level()) return false;
        return companion.distanceToSqr(owner) > square(CompanionConfig.FOLLOW_START_DISTANCE.get());
    }

    @Override
    public boolean canContinueToUse() {
        if (companion.getCommand() == CompanionEntity.Command.STAY) return false;
        if (owner == null || !owner.isAlive() || owner.level() != companion.level()) return false;
        return companion.distanceToSqr(owner) > square(CompanionConfig.FOLLOW_STOP_DISTANCE.get());
    }

    @Override
    public void start() {
        if (owner != null) companion.getNavigation().moveTo(owner, speed);
    }

    @Override
    public void stop() {
        owner = null;
        companion.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) return;
        companion.getLookControl().setLookAt(owner, 30.0F, 30.0F);
        if (companion.tickCount % 10 == 0) {
            companion.getNavigation().moveTo(owner, speed);
        }

        double teleportDistance = CompanionConfig.TELEPORT_DISTANCE.get();
        long gameTime = companion.level().getGameTime();
        if (teleportDistance > 0.0D
                && companion.distanceToSqr(owner) > square(teleportDistance)
                && gameTime >= nextTeleportTime) {
            double x = owner.getX() + (companion.getRandom().nextDouble() - 0.5D) * 2.0D;
            double z = owner.getZ() + (companion.getRandom().nextDouble() - 0.5D) * 2.0D;
            nextTeleportTime = gameTime + 100L;
            if (companion.randomTeleport(x, owner.getY(), z, true)) {
                companion.getNavigation().stop();
            }
        }
    }

    private static double square(double value) {
        return value * value;
    }
}
