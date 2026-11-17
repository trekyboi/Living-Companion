package com.livingcompanion.ai;

import com.livingcompanion.config.CompanionConfig;
import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Keeps the companion near its owner unless the owner has ordered him to stay. */
public final class FollowOwnerGoal extends Goal {
    private final CompanionEntity companion;
    private final double speed;
    private Player owner;
    private long nextTeleportTime;
    private long nextRecoveryTime;
    private Vec3 lastProgressPosition;
    private int stuckTicks;

    public FollowOwnerGoal(CompanionEntity companion, double speed) {
        this.companion = companion;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (companion.getCommand() == CompanionEntity.Command.STAY || companion.getCommand() == CompanionEntity.Command.PATROL) return false;
        owner = companion.getOwnerPlayer();
        if (owner == null || !owner.isAlive() || owner.level() != companion.level()) return false;
        return companion.distanceToSqr(owner) > square(CompanionConfig.FOLLOW_START_DISTANCE.get());
    }

    @Override
    public boolean canContinueToUse() {
        if (companion.getCommand() == CompanionEntity.Command.STAY
                || companion.getCommand() == CompanionEntity.Command.PATROL) return false;
        if (owner == null || !owner.isAlive() || owner.level() != companion.level()) return false;
        return companion.distanceToSqr(owner) > square(CompanionConfig.FOLLOW_STOP_DISTANCE.get());
    }

    @Override
    public void start() {
        lastProgressPosition = companion.position();
        stuckTicks = 0;
        if (owner != null) companion.getNavigation().moveTo(owner, companion.getCommand() == CompanionEntity.Command.RETREAT ? speed * 1.35D : speed);
    }

    @Override
    public void stop() {
        owner = null;
        lastProgressPosition = null;
        stuckTicks = 0;
        companion.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) return;
        companion.getLookControl().setLookAt(owner, 30.0F, 30.0F);

        if (companion.tickCount % 10 == 0) {
            companion.getNavigation().moveTo(owner, speed);
        }

        // Detect stalled navigation. A small position threshold ignores normal
        // collision jitter while still recognizing genuine lack of movement.
        Vec3 currentPosition = companion.position();
        if (lastProgressPosition == null
                || currentPosition.distanceToSqr(lastProgressPosition) >= 0.04D) {
            lastProgressPosition = currentPosition;
            stuckTicks = 0;
        } else {
            stuckTicks++;
        }

        double distanceToOwner = companion.distanceToSqr(owner);
        long gameTime = companion.level().getGameTime();

        // Re-path periodically when the navigator has stopped making progress.
        if (stuckTicks >= 40 && gameTime >= nextRecoveryTime) {
            nextRecoveryTime = gameTime + 40L;
            companion.getNavigation().stop();
            companion.getNavigation().moveTo(owner, speed);
        }

        // A blocked path can strand the companion several chunks behind.
        // Use Minecraft's collision-aware random teleport as a recovery fallback.
        double configuredTeleportDistance = CompanionConfig.TELEPORT_DISTANCE.get();
        if (configuredTeleportDistance > 0.0D
                && stuckTicks >= 100
                && distanceToOwner > square(8.0D)
                && gameTime >= nextTeleportTime) {
            double x = owner.getX() + (companion.getRandom().nextDouble() - 0.5D) * 2.0D;
            double z = owner.getZ() + (companion.getRandom().nextDouble() - 0.5D) * 2.0D;
            nextTeleportTime = gameTime + 100L;
            if (companion.randomTeleport(x, owner.getY(), z, true)) {
                companion.getNavigation().stop();
                lastProgressPosition = companion.position();
                stuckTicks = 0;
            }
        }

        // Preserve the existing long-distance catch-up behavior.
        if (configuredTeleportDistance > 0.0D
                && distanceToOwner > square(configuredTeleportDistance)
                && gameTime >= nextTeleportTime) {
            double x = owner.getX() + (companion.getRandom().nextDouble() - 0.5D) * 2.0D;
            double z = owner.getZ() + (companion.getRandom().nextDouble() - 0.5D) * 2.0D;
            nextTeleportTime = gameTime + 100L;
            if (companion.randomTeleport(x, owner.getY(), z, true)) {
                companion.getNavigation().stop();
                lastProgressPosition = companion.position();
                stuckTicks = 0;
            }
        }
    }

    private static double square(double value) {
        return value * value;
    }
}
