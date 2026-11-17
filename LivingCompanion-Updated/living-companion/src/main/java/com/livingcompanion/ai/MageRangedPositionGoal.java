package com.livingcompanion.ai;

import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Keeps the Mage at casting distance during combat.
 *
 * <p>The goal owns movement while a valid target is active, preventing the
 * ordinary follow goal from pulling the Mage into melee range. It closes
 * excessive gaps, retreats from nearby enemies, and holds position inside
 * the preferred 6.5-12 block band.</p>
 */
public final class MageRangedPositionGoal extends Goal {
    private static final double MIN_RANGE_SQR = 42.25D;
    private static final double MAX_RANGE_SQR = 144.0D;
    private final CompanionEntity companion;
    private int repositionCooldown;

    public MageRangedPositionGoal(CompanionEntity companion) {
        this.companion = companion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private LivingEntity target() {
        LivingEntity target = companion.getTarget();
        if (target == null || !target.isAlive() || target.level() != companion.level()) return null;
        CompanionEntity.Command command = companion.getCommand();
        if (command == CompanionEntity.Command.STAY || command == CompanionEntity.Command.RETREAT) return null;
        return target;
    }

    @Override
    public boolean canUse() {
        return target() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target() != null;
    }

    @Override
    public void start() {
        repositionCooldown = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = target();
        if (target == null) return;

        companion.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (--repositionCooldown > 0) return;
        repositionCooldown = 10;

        double distanceSqr = companion.distanceToSqr(target);
        if (distanceSqr > MAX_RANGE_SQR) {
            companion.getNavigation().moveTo(target, 1.05D);
            return;
        }
        if (distanceSqr >= MIN_RANGE_SQR) {
            companion.getNavigation().stop();
            return;
        }

        // Back away from close threats while leaving pathfinding to choose a
        // collision-safe route to the desired casting distance.
        double dx = companion.getX() - target.getX();
        double dz = companion.getZ() - target.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.001D) {
            dx = companion.getRandom().nextBoolean() ? 1.0D : -1.0D;
            dz = companion.getRandom().nextBoolean() ? 1.0D : -1.0D;
            length = Math.sqrt(dx * dx + dz * dz);
        }
        double retreatDistance = 7.5D;
        double x = companion.getX() + dx / length * retreatDistance;
        double z = companion.getZ() + dz / length * retreatDistance;
        companion.getNavigation().moveTo(x, companion.getY(), z, 1.10D);
    }

    @Override
    public void stop() {
        repositionCooldown = 0;
        companion.getNavigation().stop();
    }
}
